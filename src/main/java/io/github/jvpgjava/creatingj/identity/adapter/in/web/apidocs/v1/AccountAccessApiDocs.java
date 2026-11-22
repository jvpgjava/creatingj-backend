package io.github.jvpgjava.creatingj.identity.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.ActivateAccountRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.ConfirmPasswordResetRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.LoginRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.LoginResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PasswordResetRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PasswordResetRequestedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@Tag(name = "Account Access", description = "Ativacao de convite, login por sessao e redefinicao de senha")
public interface AccountAccessApiDocs {

    @Operation(
            operationId = "activateAccount",
            summary = "Ativar conta com token de uso unico",
            description = "Define a senha inicial com Argon2id e conclui a ativacao da conta."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Conta ativada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Token invalido, expirado ou senha fraca",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Token ja utilizado ou conta em estado invalido",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<Void> activateAccount(ActivateAccountRequest request);

    @Operation(
            operationId = "loginWithPassword",
            summary = "Autenticar conta ativa com email e senha",
            description = "Emite token de sessao opaco com protecao contra brute force e enumeracao."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas ou conta indisponivel",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "429", description = "Limite de tentativas excedido",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<LoginResponse> login(LoginRequest request);

    @Operation(
            operationId = "requestPasswordReset",
            summary = "Solicitar redefinicao de senha",
            description = "Retorna resposta neutra sem confirmar a existencia do email."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitacao aceita",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PasswordResetRequestedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Email invalido",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<PasswordResetRequestedResponse> requestPasswordReset(PasswordResetRequest request);

    @Operation(
            operationId = "confirmPasswordReset",
            summary = "Confirmar redefinicao de senha com token unico",
            description = "Atualiza o hash de senha e revoga sessoes anteriores."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha redefinida"),
            @ApiResponse(responseCode = "400", description = "Token invalido/expirado ou senha fraca",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Token ja utilizado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<Void> confirmPasswordReset(ConfirmPasswordResetRequest request);
}
