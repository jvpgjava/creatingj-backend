package io.github.jvpgjava.creatingj.web;

import io.github.jvpgjava.creatingj.bootstrap.CreatingjBackendApplication;
import io.github.jvpgjava.creatingj.support.TestInMemoryStoresConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CreatingjBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestInMemoryStoresConfig.class)
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExposeV1OpenApiGroupWithMetadataFromApiDocsInterface() throws Exception {
        String body = mockMvc.perform(get("/api-docs/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("CreatingJ API"))
                .andExpect(jsonPath("$.paths['/api/v1/system/status'].get.operationId").value("getSystemStatus"))
                .andExpect(jsonPath("$.paths['/api/v1/system/status'].get.summary")
                        .value("Consultar status basico da API v1"))
                .andExpect(jsonPath("$.paths['/api/v1/system/status'].get.tags[0]").value("System"))
                .andExpect(jsonPath("$.paths['/api/v1/system/status'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/system/status'].get.responses['500']").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode generated = objectMapper.readTree(body);
        assertThat(generated.path("paths").has("/api/v1/system/status")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/admin/users")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/admin/users/{userId}/suspend")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/activate")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/login")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/password-reset/request")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/password-reset/confirm")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/web/login")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/me")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/logout")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/sessions")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/sessions/{sessionId}")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/pkce/authorize")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/pkce/token")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/token/refresh")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/conversations")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/conversations/{conversationId}")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/conversations/{conversationId}/messages")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/conversations/{conversationId}/messages/stream")).isTrue();
        assertThat(generated.path("paths")
                .has("/api/v1/conversations/{conversationId}/generations/{generationId}/cancel")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/usage/me")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/admin/usage")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/admin/users/{userId}/quota")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/projects")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/projects/{projectId}")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/projects/{projectId}/knowledge/sources")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/projects/{projectId}/knowledge/sources/{sourceId}")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/projects/{projectId}/knowledge/search")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/projects/{projectId}/knowledge/jobs")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/projects/{projectId}/agent/runs")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/projects/{projectId}/agent/runs/{runId}")).isTrue();
        assertThat(generated.path("paths")
                .has("/api/v1/projects/{projectId}/agent/runs/{runId}/proposals/{proposalId}/decision")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/projects/{projectId}/agent/runs/{runId}/cancel")).isTrue();
        assertThat(generated.path("components").path("securitySchemes").has("bearerAuth")).isTrue();
        assertThat(generated.path("components").path("securitySchemes").has("cookieAuth")).isTrue();
        assertThat(generated.path("paths").has("/api/v1/auth/register")).isFalse();
    }

    @Test
    void shouldExposeSwaggerUiAtDedicatedPathInNonProdProfile() throws Exception {
        var result = mockMvc.perform(get("/api-docs/ui"))
                .andReturn();

        int statusCode = result.getResponse().getStatus();
        assertThat(statusCode).isIn(200, 302);
        if (statusCode == 302) {
            String location = result.getResponse().getHeader("Location");
            assertThat(location).isNotBlank();
            mockMvc.perform(get(location))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldMatchCommittedOpenApiV1Snapshot() throws Exception {
        Path snapshotPath = Path.of("openapi", "openapi-v1.yaml");
        assertThat(Files.exists(snapshotPath)).isTrue();
        String snapshotContent = Files.readString(snapshotPath);

        String generatedYaml = mockMvc.perform(get("/api-docs.yaml/v1"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(normalizeLineEndings(generatedYaml)).isEqualTo(normalizeLineEndings(snapshotContent));
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n").trim();
    }
}
