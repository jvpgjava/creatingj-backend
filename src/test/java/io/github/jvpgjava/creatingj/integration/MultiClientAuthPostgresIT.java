package io.github.jvpgjava.creatingj.integration;

import io.github.jvpgjava.creatingj.bootstrap.CreatingjBackendApplication;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.PasswordHasherPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CreatingjBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("postgres-it")
@EnabledIfEnvironmentVariable(named = "CREATINGJ_DB_URL", matches = "jdbc:postgresql:.*")
class MultiClientAuthPostgresIT {

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

    @BeforeEach
    void resetAndSeedUsers() {
        jdbcTemplate.execute(
                "TRUNCATE TABLE authorization_codes, audit_events, sessions, invitations, user_roles, users CASCADE");

        userStore.save(User.active(
                UserId.newId(),
                Email.of("alice@creatingj.dev"),
                "Alice Dev",
                passwordHasher.hash("AliceStr0ng!2026"),
                Set.of(Role.MEMBER),
                null,
                clock.now()
        ));
        userStore.save(User.active(
                UserId.newId(),
                Email.of("bob@creatingj.dev"),
                "Bob Dev",
                passwordHasher.hash("BobStr0ng!2026"),
                Set.of(Role.MEMBER),
                null,
                clock.now()
        ));
    }

    @Test
    void shouldSupportWebCookieLoginCsrfProtectionMeAndLogout() throws Exception {
        var loginResult = mockMvc.perform(post("/api/v1/auth/web/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@creatingj.dev\",\"password\":\"AliceStr0ng!2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@creatingj.dev"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        List<String> setCookies = loginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).anyMatch(c -> c.startsWith("CREATINGJ_SESSION=") && c.contains("HttpOnly")
                && c.contains("SameSite=Lax"));
        assertThat(setCookies).anyMatch(c -> c.startsWith("XSRF-TOKEN=") && !c.contains("HttpOnly"));

        Cookie sessionCookie = extractCookie(setCookies, "CREATINGJ_SESSION");
        Cookie xsrfCookie = extractCookie(setCookies, "XSRF-TOKEN");

        mockMvc.perform(get("/api/v1/auth/me").cookie(sessionCookie, xsrfCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@creatingj.dev"))
                .andExpect(jsonPath("$.displayName").value("Alice Dev"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString("CRJ_API_KEY"))));

        mockMvc.perform(post("/api/v1/auth/logout").cookie(sessionCookie, xsrfCookie))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(sessionCookie, xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").cookie(sessionCookie, xsrfCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldSupportPkceS256RefreshRotationReuseDetectionAndCrossUserIsolation() throws Exception {
        String aliceLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@creatingj.dev\",\"password\":\"AliceStr0ng!2026\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String aliceToken = objectMapper.readTree(aliceLogin).path("accessToken").asString();

        String bobLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bob@creatingj.dev\",\"password\":\"BobStr0ng!2026\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String bobToken = objectMapper.readTree(bobLogin).path("accessToken").asString();

        String verifier = "creatingj-vscode-pkce-verifier-with-sufficient-entropy-2026";
        String challenge = sha256Base64Url(verifier);

        String authBody = mockMvc.perform(post("/api/v1/auth/pkce/authorize")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clientId": "creatingj-vscode",
                                  "redirectUri": "http://127.0.0.1/callback",
                                  "codeChallenge": "%s",
                                  "codeChallengeMethod": "S256",
                                  "state": "opaque-state-1"
                                }
                                """.formatted(challenge)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationCode").isNotEmpty())
                .andExpect(jsonPath("$.state").value("opaque-state-1"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String authCode = objectMapper.readTree(authBody).path("authorizationCode").asString();
        String storedCodeHash = jdbcTemplate.queryForObject(
                "SELECT code_hash FROM authorization_codes LIMIT 1", String.class);
        assertThat(storedCodeHash).hasSize(64).isNotEqualTo(authCode);

        String tokenBody = mockMvc.perform(post("/api/v1/auth/pkce/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "clientId": "creatingj-vscode",
                                  "redirectUri": "http://127.0.0.1/callback",
                                  "codeVerifier": "%s"
                                }
                                """.formatted(authCode, verifier)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode pkceTokens = objectMapper.readTree(tokenBody);
        String pkceAccess = pkceTokens.path("accessToken").asString();
        String pkceRefresh = pkceTokens.path("refreshToken").asString();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + pkceAccess))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@creatingj.dev"));

        String refreshedBody = mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(pkceRefresh)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String rotatedAccess = objectMapper.readTree(refreshedBody).path("accessToken").asString();

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(pkceRefresh)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + rotatedAccess))
                .andExpect(status().isUnauthorized());

        Integer reuseAudits = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_events WHERE action = 'REFRESH_REUSE_DETECTED'",
                Integer.class
        );
        assertThat(reuseAudits).isEqualTo(1);

        String bobSessionsBody = mockMvc.perform(get("/api/v1/auth/sessions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String bobSessionId = objectMapper.readTree(bobSessionsBody).get(0).path("id").asString();

        mockMvc.perform(delete("/api/v1/auth/sessions/{sessionId}", bobSessionId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + aliceToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bob@creatingj.dev"));
    }

    @Test
    void shouldEnforceStrictCorsAllowlist() throws Exception {
        mockMvc.perform(options("/api/v1/auth/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        mockMvc.perform(options("/api/v1/auth/me")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    private static Cookie extractCookie(List<String> setCookies, String name) {
        String raw = setCookies.stream()
                .filter(c -> c.startsWith(name + "="))
                .findFirst()
                .orElseThrow();
        String value = raw.substring((name + "=").length()).split(";")[0];
        return new Cookie(name, value);
    }

    private static String sha256Base64Url(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
