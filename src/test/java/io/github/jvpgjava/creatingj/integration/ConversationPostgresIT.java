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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CreatingjBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("postgres-it")
@EnabledIfEnvironmentVariable(named = "CREATINGJ_DB_URL", matches = "jdbc:postgresql:.*")
class ConversationPostgresIT {

    private static final String TEST_API_KEY = "crj-it-secret-key-never-expose-777";
    private static final AtomicInteger MOCK_STATUS = new AtomicInteger(200);
    private static final AtomicReference<String> MOCK_MODE = new AtomicReference<>("normal");

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

    private String aliceToken;
    private String bobToken;

    @BeforeAll
    static void startMockProvider() throws Exception {
        mockProviderServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockProviderServer.createContext("/v1/chat/completions", exchange -> {
            String reqBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int status = MOCK_STATUS.get();
            if (status != 200) {
                byte[] err = ("{\"error\":\"upstream failed with key " + TEST_API_KEY + "\"}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, err.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(err);
                }
                return;
            }
            if (reqBody.contains("\"stream\":true")) {
                byte[] sse = """
                        data: {"model":"z-ai/glm-5.3-flash","choices":[{"delta":{"content":"","reasoning":"ok"}}]}

                        data: {"model":"z-ai/glm-5.3-flash","choices":[{"delta":{"content":"Ola "}}]}

                        data: {"model":"z-ai/glm-5.3-flash","choices":\
                        [{"delta":{"content":"CreatingJ!"},"finish_reason":"stop"}]}

                        data: {"model":"z-ai/glm-5.3-flash","choices":[],\
                        "usage":{"prompt_tokens":14,"completion_tokens":8,"total_tokens":22}}

                        data: [DONE]

                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
                exchange.sendResponseHeaders(200, sse.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(sse);
                }
            } else {
                byte[] json = """
                        {
                          "model": "z-ai/glm-5.3-flash",
                          "choices": [{"message": {"role": "assistant", \
                        "content": "Resposta sincronizada"}, "finish_reason": "stop"}],
                          "usage": {"prompt_tokens": 12, "completion_tokens": 9, "total_tokens": 21}
                        }
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, json.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(json);
                }
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
        registry.add("creatingj.provider.ai.api-key", () -> TEST_API_KEY);
        registry.add("creatingj.provider.ai.strict-url-validation", () -> "false");
    }

    @BeforeEach
    void resetAndAuthenticateUsers() throws Exception {
        MOCK_STATUS.set(200);
        MOCK_MODE.set("normal");
        jdbcTemplate.execute(
                "TRUNCATE TABLE generations, messages, conversations, authorization_codes, "
                        + "audit_events, sessions, invitations, user_roles, users CASCADE");

        userStore.save(User.active(
                UserId.newId(), Email.of("alice.chat@creatingj.dev"), "Alice Chat",
                passwordHasher.hash("AliceStr0ng!2026"), Set.of(Role.MEMBER), null, clock.now()));
        userStore.save(User.active(
                UserId.newId(), Email.of("bob.chat@creatingj.dev"), "Bob Chat",
                passwordHasher.hash("BobStr0ng!2026"), Set.of(Role.MEMBER), null, clock.now()));

        aliceToken = login("alice.chat@creatingj.dev", "AliceStr0ng!2026");
        bobToken = login("bob.chat@creatingj.dev", "BobStr0ng!2026");
    }

    @Test
    void shouldPersistChatStreamSseEnforceCrossUserIsolationAndSanitizeProviderErrors() throws Exception {
        String createdBody = mockMvc.perform(post("/api/v1/conversations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Arquitetura Hexagonal\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Arquitetura Hexagonal"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String conversationId = objectMapper.readTree(createdBody).path("id").asString();

        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Explique portas e adaptadores\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantMessage.content").value("Resposta sincronizada"))
                .andExpect(jsonPath("$.generation.status").value("COMPLETED"))
                .andExpect(jsonPath("$.generation.totalTokens").value(21))
                .andExpect(content().string(not(containsString(TEST_API_KEY))));

        MvcResult asyncResult = mockMvc.perform(post("/api/v1/conversations/{id}/messages/stream", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Teste SSE\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String ssePayload = mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(ssePayload)
                .contains("event:generation.started")
                .contains("event:generation.delta")
                .contains("Ola ")
                .contains("CreatingJ!")
                .contains("event:generation.completed")
                .doesNotContain(TEST_API_KEY);

        mockMvc.perform(get("/api/v1/conversations/{id}", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(4))
                .andExpect(jsonPath("$.messages[3].content").value("Ola CreatingJ!"));

        mockMvc.perform(get("/api/v1/conversations/{id}", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Tentativa BOLA\"}"))
                .andExpect(status().isNotFound());

        MOCK_STATUS.set(401);
        mockMvc.perform(post("/api/v1/conversations/{id}/messages", conversationId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Forcar erro upstream\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_GATEWAY"))
                .andExpect(content().string(not(containsString(TEST_API_KEY))));
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.path("accessToken").asString();
    }
}
