package io.github.jvpgjava.creatingj.inference.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1.AdminUsageOverviewResponse;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1.UpdateUserQuotaRequest;
import io.github.jvpgjava.creatingj.inference.adapter.in.web.dto.v1.UserQuotaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin Usage", description = "Visao administrativa de consumo e configuracao de quotas (restrito a ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "cookieAuth")
public interface AdminUsageApiDocs {

    @Operation(
            operationId = "getAdminUsageOverview",
            summary = "Consultar consumo agregado diario da plataforma"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Visao agregada de consumo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AdminUsageOverviewResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Sem privilegio administrativo",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<AdminUsageOverviewResponse> getUsageOverview(AuthenticatedUser actor);

    @Operation(
            operationId = "updateUserQuota",
            summary = "Configurar quotas diarias e limite de concorrencia de um usuario"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quota atualizada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserQuotaResponse.class))),
            @ApiResponse(responseCode = "400", description = "Limites invalidos",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Sem privilegio administrativo",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<UserQuotaResponse> updateUserQuota(
            String userId,
            UpdateUserQuotaRequest request,
            AuthenticatedUser actor
    );
}
