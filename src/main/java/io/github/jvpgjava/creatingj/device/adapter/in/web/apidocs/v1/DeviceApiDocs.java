package io.github.jvpgjava.creatingj.device.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.CreatePairingChallengeRequest;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.DeviceSummaryResponse;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.DevicesResponse;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.PairingChallengeResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@Tag(name = "Devices", description = "Pareamento, presenca e revogacao de dispositivos autorizados")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "cookieAuth")
public interface DeviceApiDocs {

    @Operation(
            operationId = "createPairingChallenge",
            summary = "Gerar codigo de pareamento de uso unico",
            description = "Emite um codigo temporario para parear o agente local via POST /api/v1/runner/pair/complete."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Codigo gerado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PairingChallengeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Entrada invalida",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<PairingChallengeResponse> createPairingChallenge(
            CreatePairingChallengeRequest request,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "listDevices",
            summary = "Listar dispositivos pareados e status online do usuario",
            description = "Exibe dispositivos ativos ou revogados do usuario e indica presenca com base no heartbeat."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de dispositivos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DevicesResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<DevicesResponse> listDevices(AuthenticatedUser actor);

    @Operation(
            operationId = "revokeDevice",
            summary = "Revogar dispositivo e invalidar suas credenciais",
            description = "Invalida imediatamente o X-Device-Token do dispositivo e impede novos leases de tarefa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispositivo revogado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DeviceSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Dispositivo nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<DeviceSummaryResponse> revokeDevice(
            @Parameter(description = "Identificador UUID do dispositivo",
                    example = "550e8400-e29b-41d4-a716-446655440050")
            String deviceId,
            AuthenticatedUser actor
    );
}
