package io.github.jvpgjava.creatingj.common.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.common.adapter.in.web.dto.v1.SystemStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@Tag(name = "System", description = "Diagnostico basico e metadados publicos da API v1")
public interface SystemApiDocs {

    @Operation(
            operationId = "getSystemStatus",
            summary = "Consultar status basico da API v1",
            description = "Retorna metadados operacionais nao sensiveis da API v1."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Servico operacional",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SystemStatusResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno sanitizado em RFC 9457",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    ResponseEntity<SystemStatusResponse> getStatus();
}
