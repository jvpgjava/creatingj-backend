package io.github.jvpgjava.creatingj.integration;

import io.github.jvpgjava.creatingj.bootstrap.CreatingjBackendApplication;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.PasswordHasherPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
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
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CreatingjBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("postgres-it")
@EnabledIfEnvironmentVariable(named = "CREATINGJ_DB_URL", matches = "jdbc:postgresql:.*")
class BetaHardeningAndLoadPostgresIT {

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
    private String adminToken;
    private String memberToken;

    @BeforeEach
    void resetAndSeed() throws Exception {
        jdbcTemplate.execute(
                "TRUNCATE TABLE tool_approvals, task_events, remote_tasks, device_credentials, "
                        + "device_pairing_challenges, devices, agent_tool_proposals, agent_runs, "
                        + "knowledge_index_jobs, knowledge_chunks, knowledge_sources, projects, "
                        + "usage_records, user_quotas, generations, messages, conversations, "
                        + "authorization_codes, audit_events, sessions, invitations, user_roles, users CASCADE");

        adminUser = userStore.save(User.active(
                UserId.newId(), Email.of("admin.beta@creatingj.dev"), "Admin Beta",
                passwordHasher.hash("AdminStr0ng!2026"), Set.of(Role.ADMIN), null, clock.now()));
        userStore.save(User.active(
                UserId.newId(), Email.of("member.beta@creatingj.dev"), "Member Beta",
                passwordHasher.hash("MemberStr0ng!2026"), Set.of(Role.MEMBER), null, clock.now()));

        adminToken = login("admin.beta@creatingj.dev", "AdminStr0ng!2026");
        memberToken = login("member.beta@creatingj.dev", "MemberStr0ng!2026");
    }

    @Test
    void shouldEnforceDataRetentionSecurityHeadersSqliDefenseAndControlledConcurrentLoad() throws Exception {
        mockMvc.perform(post("/api/v1/admin/maintenance/retention")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        UUID oldChallengeId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO device_pairing_challenges (
                    id, owner_user_id, device_name, platform, code_hash, expires_at, consumed_at, created_at
                )
                VALUES (?, ?, 'Old PC', 'LINUX', ?, NOW() - INTERVAL '15 days', NULL, NOW() - INTERVAL '15 days')
                """, oldChallengeId, adminUser.id().value(), "expired-hash-" + oldChallengeId);

        mockMvc.perform(post("/api/v1/admin/maintenance/retention")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(jsonPath("$.deletedPairingChallenges").value(1));

        Integer remainingChallenge = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM device_pairing_challenges WHERE id = ?",
                Integer.class,
                oldChallengeId
        );
        assertThat(remainingChallenge).isZero();

        mockMvc.perform(post("/api/v1/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text not allowed"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));

        String projectBody = mockMvc.perform(post("/api/v1/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Load Bench\",\"description\":\"Benchmark\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(projectBody).path("id").asString();

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/knowledge/sources")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourcePath\":\"docs/a.md\",\"title\":\"A\",\"content\":\"PostgreSQL benchmark\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/knowledge/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"'; DROP TABLE users; --\",\"topK\":5}"))
                .andExpect(status().isOk());

        Integer userCountAfterSqli = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        assertThat(userCountAfterSqli).isEqualTo(2);

        int totalRequests = 40;
        List<Long> latenciesMs = Collections.synchronizedList(new ArrayList<>());
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            List<Callable<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < totalRequests; i++) {
                final int idx = i;
                tasks.add(() -> {
                    long start = System.nanoTime();
                    int code;
                    if (idx % 2 == 0) {
                        code = mockMvc.perform(get("/api/v1/projects")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken))
                                .andReturn().getResponse().getStatus();
                    } else {
                        code = mockMvc.perform(post("/api/v1/projects/" + projectId + "/knowledge/search")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + memberToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"query\":\"PostgreSQL benchmark\",\"topK\":3}"))
                                .andReturn().getResponse().getStatus();
                    }
                    latenciesMs.add((System.nanoTime() - start) / 1_000_000L);
                    return code;
                });
            }
            List<Future<Integer>> futures = pool.invokeAll(tasks);
            for (Future<Integer> future : futures) {
                assertThat(future.get()).isEqualTo(200);
            }
        }
        Collections.sort(latenciesMs);
        long p95 = latenciesMs.get((int) Math.floor(0.95 * (latenciesMs.size() - 1)));
        assertThat(latenciesMs).hasSize(totalRequests);
        assertThat(p95).isLessThan(1000L);
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("accessToken").asString();
    }
}
