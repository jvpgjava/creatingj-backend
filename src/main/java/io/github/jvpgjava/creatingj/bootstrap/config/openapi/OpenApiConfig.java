package io.github.jvpgjava.creatingj.bootstrap.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiConfig {

    @Bean
    public OpenAPI creatingjOpenApi() {
        return new OpenAPI()
                .addServersItem(new Server().url("/").description("Same origin"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .description("Token de sessao opaco emitido em /api/v1/auth/login ou PKCE"))
                        .addSecuritySchemes("cookieAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("CREATINGJ_SESSION")
                                .description("Cookie HttpOnly emitido em /api/v1/auth/web/login")))
                .info(new Info()
                        .title("CreatingJ API")
                        .description("Contrato versionado da plataforma CreatingJ")
                        .version("v1"));
    }

    @Bean
    public GroupedOpenApi v1OpenApiGroup() {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/api/v1/**")
                .packagesToScan("io.github.jvpgjava.creatingj")
                .build();
    }
}
