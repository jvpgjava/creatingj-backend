package io.github.jvpgjava.creatingj.identity.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.CurrentUserResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.LoginRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PkceAuthorizeRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PkceAuthorizeResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PkceTokenRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.RefreshTokenRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.SessionSummaryResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.TokenPairResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.WebLoginResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Tag(name = "Multi-Client Auth", description = "Sessao web por cookie/CSRF, PKCE S256 para apps publicos e sessoes")
public interface MultiClientAuthApiDocs {

    @Operation(
            operationId = "webCookieLogin",
            summary = "Autenticar na web emitindo cookie HttpOnly + XSRF-TOKEN",
            description = "Autentica conta ativa para o navegador sem expor token no corpo JSON."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessao web iniciada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = WebLoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "429", description = "Limite de tentativas excedido",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<WebLoginResponse> webLogin(LoginRequest request);

    @Operation(
            operationId = "getCurrentUser",
            summary = "Consultar perfil e sessao do usuario autenticado",
            description = "Retorna metadados da conta e da sessao atual sem credenciais ou segredos de IA.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "cookieAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CurrentUserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<CurrentUserResponse> me(AuthenticatedUser actor);

    @Operation(
            operationId = "logoutCurrentSession",
            summary = "Encerrar sessao atual e limpar cookies",
            description = "Revoga imediatamente a sessao corrente no servidor.",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "cookieAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sessao encerrada"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Token CSRF ausente ou invalido",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<Void> logout(AuthenticatedUser actor);

    @Operation(
            operationId = "listOwnSessions",
            summary = "Listar sessoes ativas do proprio usuario",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "cookieAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sessoes ativas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = SessionSummaryResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<List<SessionSummaryResponse>> listSessions(AuthenticatedUser actor);

    @Operation(
            operationId = "revokeOwnSession",
            summary = "Revogar uma sessao especifica do proprio usuario",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "cookieAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sessao revogada"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Sessao nao encontrada para este usuario",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<Void> revokeSession(String sessionId, AuthenticatedUser actor);

    @Operation(
            operationId = "authorizePkceClient",
            summary = "Emitir codigo de autorizacao PKCE S256 para cliente publico",
            security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "cookieAuth")}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Codigo de autorizacao emitido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PkceAuthorizeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Cliente, redirectUri ou codeChallenge invalido",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<PkceAuthorizeResponse> authorizePkce(PkceAuthorizeRequest request, AuthenticatedUser actor);

    @Operation(
            operationId = "exchangePkceCode",
            summary = "Trocar codigo de autorizacao + code_verifier por accessToken e refreshToken"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Par de tokens emitido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenPairResponse.class))),
            @ApiResponse(responseCode = "401", description = "Validacao PKCE ou codigo invalido",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Codigo ja utilizado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<TokenPairResponse> exchangePkceToken(PkceTokenRequest request);

    @Operation(
            operationId = "refreshSessionToken",
            summary = "Rotacionar refreshToken emitindo novo par de tokens",
            description = "Detecta reuso de refreshToken rotacionado e revoga toda a familia de sessoes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Novo par de tokens emitido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenPairResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token invalido, expirado ou reutilizado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<TokenPairResponse> refreshToken(RefreshTokenRequest request);
}
