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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CreatingjBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("postgres-it")
@EnabledIfEnvironmentVariable(named = "CREATINGJ_DB_URL", matches = "jdbc:postgresql:.*")
class RagProjectKnowledgePostgresIT {

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
                "TRUNCATE TABLE knowledge_index_jobs, knowledge_chunks, knowledge_sources, projects, "
                        + "usage_records, user_quotas, generations, messages, conversations, "
                        + "authorization_codes, audit_events, sessions, invitations, user_roles, users CASCADE");

        userStore.save(User.active(
                UserId.newId(), Email.of("alice.rag@creatingj.dev"), "Alice RAG",
                passwordHasher.hash("AliceStr0ng!2026"), Set.of(Role.MEMBER), null, clock.now()));
        userStore.save(User.active(
                UserId.newId(), Email.of("bob.rag@creatingj.dev"), "Bob RAG",
                passwordHasher.hash("BobStr0ng!2026"), Set.of(Role.MEMBER), null, clock.now()));

        tokenUserA = login("alice.rag@creatingj.dev", "AliceStr0ng!2026");
        tokenUserB = login("bob.rag@creatingj.dev", "BobStr0ng!2026");
    }

    @Test
    void shouldPersistPgvectorRagWithStrictOwnerProjectIsolationDeduplicationAndDeletion() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String projectAResp = mockMvc.perform(post("/api/v1/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"CreatingJ Backend\",\"description\":\"Arquitetura hexagonal\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("creatingj-backend"))
                .andReturn().getResponse().getContentAsString();
        String projectAId = objectMapper.readTree(projectAResp).path("id").asString();

        String projectBResp = mockMvc.perform(post("/api/v1/projects")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bob Private Repo\",\"description\":\"Confidencial\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String projectBId = objectMapper.readTree(projectBResp).path("id").asString();

        String archPayload = """
                {
                  "sourcePath": "docs/architecture.md",
                  "title": "Arquitetura Hexagonal",
                  "content": "O backend CreatingJ usa Java 25, Spring Boot 4.2, PostgreSQL 18.6 e pgvector \
                com indice HNSW para busca vetorial isolada por projeto."
                }
                """;
        String ingestArchResp = mockMvc.perform(post("/api/v1/projects/" + projectAId + "/knowledge/sources")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(archPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deduplicated").value(false))
                .andExpect(jsonPath("$.source.status").value("INDEXED"))
                .andExpect(jsonPath("$.source.chunkCount").value(1))
                .andExpect(jsonPath("$.job.status").value("COMPLETED"))
                .andReturn().getResponse().getContentAsString();
        String archSourceId = objectMapper.readTree(ingestArchResp).path("source").path("id").asString();

        mockMvc.perform(post("/api/v1/projects/" + projectAId + "/knowledge/sources")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(archPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deduplicated").value(true))
                .andExpect(jsonPath("$.job.status").value("SKIPPED_UNCHANGED"));

        mockMvc.perform(post("/api/v1/projects/" + projectAId + "/knowledge/sources")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourcePath": "docs/deploy.md",
                                  "title": "Deploy VPS",
                                  "content": "O deploy em VPS Linux utiliza systemd, Nginx proxy reverso \
                                com TLS e Jenkins sem Docker."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deduplicated").value(false));

        mockMvc.perform(post("/api/v1/projects/" + projectBId + "/knowledge/sources")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourcePath": "docs/secret-bob.md",
                                  "title": "Confidencial Bob",
                                  "content": "SEGREDO_EXCLUSIVO_BOB PostgreSQL 18.6 pgvector HNSW \
                                arquitetura hexagonal."
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/projects/" + projectAId + "/knowledge/sources")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourcePath": ".env",
                                  "title": "Env",
                                  "content": "APP_PORT=8080"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(post("/api/v1/projects/" + projectAId + "/knowledge/sources")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourcePath": "docs/leak.md",
                                  "title": "Leak",
                                  "content": "Chave vazada sk-1234567890abcdefghijklmnop"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        String searchBody = mockMvc.perform(post("/api/v1/projects/" + projectAId + "/knowledge/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"PostgreSQL pgvector indice HNSW\",\"topK\":5,\"minScore\":0.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matches[0].sourcePath").value("docs/architecture.md"))
                .andReturn().getResponse().getContentAsString();

        JsonNode searchJson = objectMapper.readTree(searchBody);
        assertThat(searchBody).doesNotContain("SEGREDO_EXCLUSIVO_BOB");
        assertThat(searchJson.path("latencyMs").asLong()).isLessThan(2000L);

        mockMvc.perform(get("/api/v1/projects/" + projectAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(post("/api/v1/projects/" + projectAId + "/knowledge/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"pgvector\",\"topK\":5}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(get("/api/v1/projects/" + projectAId + "/knowledge/jobs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3));

        mockMvc.perform(delete("/api/v1/projects/" + projectAId + "/knowledge/sources/" + archSourceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isNoContent());

        Integer remainingArchChunks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunks WHERE source_id = ?",
                Integer.class,
                UUID.fromString(archSourceId)
        );
        assertThat(remainingArchChunks).isZero();

        String searchAfterDelete = mockMvc.perform(post("/api/v1/projects/" + projectAId + "/knowledge/search")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"PostgreSQL pgvector indice HNSW\",\"topK\":5,\"minScore\":0.0}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(searchAfterDelete).doesNotContain("docs/architecture.md");

        mockMvc.perform(delete("/api/v1/projects/" + projectAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenUserA))
                .andExpect(status().isNoContent());

        Integer remainingProjectAChunks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_chunks WHERE project_id = ?",
                Integer.class,
                UUID.fromString(projectAId)
        );
        assertThat(remainingProjectAChunks).isZero();
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
