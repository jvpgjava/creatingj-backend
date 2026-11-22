package io.github.jvpgjava.creatingj.integration;

import com.sun.net.httpserver.HttpServer;
import io.github.jvpgjava.creatingj.bootstrap.CreatingjBackendApplication;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.PasswordHasherPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CreatingjBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("postgres-it")
@EnabledIfEnvironmentVariable(named = "CREATINGJ_DB_URL", matches = "jdbc:postgresql:.*")
class UsageAndQuotasPostgresIT {

    private static HttpServer mockProviderServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserStorePort userStore;

    @Autowired
    private PasswordHasherPort passwordHasher;

    @Autowired
    private ClockPort clock;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User memberUser;
    private String adminToken;
    private String memberToken;

    @BeforeAll
    static void startMockProvider() throws Exception {
        mockProviderServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockProviderServer.createContext("/v1/chat/completions", exchange -> {
            byte[] json = """
                    {
                      "model": "z-ai/glm-5.3-flash",
                      "choices": [{"message": {"role": "assistant", "content": "SEGREDO_RESPOSTA_PRIVADA"}, \
                    "finish_reason": "stop"}],
                      "usage": {"prompt_tokens": 11, "completion_tokens": 10, "total_tokens": 21}
                    }
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(json);
            }
        });
        mockProviderServer.start();
    }

    @AfterAll
    static void stopMockProvider() {
        if (mockProviderServer != null) {
            mockProviderServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void configureMockProvider(DynamicPropertyRegistry registry) {
        registry.add("creatingj.provider.ai.base-url",
                () -> "http://127.0.0.1:" + mockProviderServer.getAddress().getPort() + "/v1");
        registry.add("creatingj.provider.ai.api-key", () -> "crj-quota-it-key");
        registry.add("creatingj.provider.ai.strict-url-validation", () -> "false");
    }

    @BeforeEach
    void resetAndSeed() throws Exception {
        jdbcTemplate.execute(
                "TRUNCATE TABLE usage_records, user_quotas, generations, messages, conversations, "
                        + "authorization_codes, audit_events, sessions, invitations, user_roles, users CASCADE");

        adminUser = userStore.save(User.active(
                UserId.newId(), Email.of("admin.quota@creatingj.dev"), "Admin Quota",
                passwordHasher.hash("AdminStr0ng!2026"), Set.of(Role.ADMIN), null, clock.now()));
        memberUser = userStore.save(User.active(
                UserId.newId(), Email.of("member.quota@creatingj.dev"), "Member Quota",
                passwordHasher.hash("MemberStr0ng!2026"), Set.of(Role.MEMBER), adminUser.id(), clock.now()));

        adminToken = login("admin.quota@creatingj.dev", "AdminStr0ng!2026");
        memberToken = login("member.quota@creatingj.dev", "MemberStr0ng!2026");
    }

    @Test
    void shouldTrackUsageEnforceConfiguredQuotasAndProtectAdminMetricsAndUsageEndpoints() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/{userId}/quota", memberUser.id().value())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyTokenLimit\":20,\"dailyRequestLimit\":5,\"maxConcurrentGenerations\":1}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/admin/users/{userId}/quota", memberUser.id().value())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dailyTokenLimit\":20,\"dailyRequestLimit\":5,\"maxConcurrentGenerations\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyTokenLimit").value(20));

        String convBody = mockMvc.perform(post("/api/v1/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Teste Quotas\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String convId = objectMapper.readTree(convBody).path("id").asString();

        mockMvc.perform(post("/api/v1/conversations/{id}/messages", convId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"SEGREDO_PROMPT_PRIVADO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generation.totalTokens").value(21));

        mockMvc.perform(get("/api/v1/usage/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usedTokens").value(21))
                .andExpect(jsonPath("$.usedRequests").value(1))
                .andExpect(jsonPath("$.remainingTokens").value(0))
                .andExpect(jsonPath("$.recentRecords.length()").value(1))
                .andExpect(content().string(not(containsString("SEGREDO_PROMPT_PRIVADO"))))
                .andExpect(content().string(not(containsString("SEGREDO_RESPOSTA_PRIVADA"))));

        mockMvc.perform(post("/api/v1/conversations/{id}/messages", convId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Segunda tentativa bloqueada\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("QUOTA_EXCEEDED"));

        Integer quotaAudits = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE action = 'QUOTA_EXCEEDED'", Integer.class);
        assertThat(quotaAudits).isEqualTo(1);

        mockMvc.perform(get("/api/v1/admin/usage")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/usage")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTokensToday").value(21))
                .andExpect(jsonPath("$.totalRequestsToday").value(1));

        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/metrics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/metrics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("creatingj.inference.requests")));
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).path("accessToken").asString();
    }
}
