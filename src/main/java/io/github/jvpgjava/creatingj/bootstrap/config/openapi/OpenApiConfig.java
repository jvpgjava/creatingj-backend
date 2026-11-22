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

    private static final String API_DESCRIPTION = """
            Contrato HTTP versionado da plataforma CreatingJ (v1).

            ### Como testar pelo Swagger UI
            1. Autentique via `POST /api/v1/auth/login` para obter um `accessToken` opaco.
            2. Clique em **Authorize**, informe o token em `bearerAuth` e execute os endpoints protegidos.
            3. Para fluxo web com cookie (`POST /api/v1/auth/web/login`), envie o header `X-XSRF-TOKEN` \
            em mutacoes (`POST`, `PUT`, `PATCH`, `DELETE`).
            4. Endpoints `/api/v1/runner/**` exigem exclusivamente o header `X-Device-Token` emitido no pareamento.

            ### Padroes do contrato
            - Erros seguem RFC 9457 (`application/problem+json`) com `errorCode` e `traceId`.
            - Endpoints de streaming utilizam `text/event-stream` (SSE).
            """;

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
                                .description("Cookie HttpOnly emitido em /api/v1/auth/web/login"))
                        .addSecuritySchemes("deviceTokenAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Device-Token")
                                .description("Credencial exclusiva do dispositivo pareado (ROLE_RUNNER)")))
                .info(new Info()
                        .title("CreatingJ API")
                        .description(API_DESCRIPTION)
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
