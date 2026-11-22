package io.github.jvpgjava.creatingj.web;

import io.github.jvpgjava.creatingj.bootstrap.CreatingjBackendApplication;
import io.github.jvpgjava.creatingj.support.TestInMemoryStoresConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CreatingjBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@Import(TestInMemoryStoresConfig.class)
@TestPropertySource(properties = {
        "creatingj.provider.ai.api-key=prod-test-placeholder-only",
        "creatingj.persistence.jdbc.enabled=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class ProdSecurityProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldBlockOpenApiAndSwaggerUiInProdProfile() throws Exception {
        int docsStatus = mockMvc.perform(get("/api-docs")).andReturn().getResponse().getStatus();
        int docsV1Status = mockMvc.perform(get("/api-docs/v1")).andReturn().getResponse().getStatus();
        int docsYamlV1Status = mockMvc.perform(get("/api-docs.yaml/v1")).andReturn().getResponse().getStatus();
        int uiStatus = mockMvc.perform(get("/api-docs/ui")).andReturn().getResponse().getStatus();

        assertThat(docsStatus).isIn(401, 403, 404);
        assertThat(docsV1Status).isIn(401, 403, 404);
        assertThat(docsYamlV1Status).isIn(401, 403, 404);
        assertThat(uiStatus).isIn(401, 403, 404);
    }

    @Test
    void shouldKeepActuatorHealthMinimalAndProtectOtherActuatorEndpointsInProd() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());

        int envStatus = mockMvc.perform(get("/actuator/env")).andReturn().getResponse().getStatus();
        assertThat(envStatus).isIn(401, 403, 404);
    }
}
