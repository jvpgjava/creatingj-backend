package io.github.jvpgjava.creatingj.device.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.CompletePairingRequest;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.CompletePairingResponse;
import io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1.DeviceSummaryResponse;
import io.github.jvpgjava.creatingj.device.adapter.in.web.security.AuthenticatedDevice;
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

@Tag(name = "Runner Protocol", description = "Endpoints exclusivos do runner local (creatingj-agent)")
public interface RunnerDeviceApiDocs {

    @Operation(
            operationId = "completeDevicePairing",
            summary = "Trocar codigo de pareamento de uso unico por credencial de dispositivo"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dispositivo pareado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CompletePairingResponse.class))),
            @ApiResponse(responseCode = "401", description = "Codigo invalido ou expirado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<CompletePairingResponse> completePairing(CompletePairingRequest request);

    @Operation(
            operationId = "recordDeviceHeartbeat",
            summary = "Registrar heartbeat de presenca do dispositivo autenticado",
            security = @SecurityRequirement(name = "deviceTokenAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Heartbeat registrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = DeviceSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Credencial de dispositivo invalida ou revogada",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<DeviceSummaryResponse> heartbeat(AuthenticatedDevice device);
}
