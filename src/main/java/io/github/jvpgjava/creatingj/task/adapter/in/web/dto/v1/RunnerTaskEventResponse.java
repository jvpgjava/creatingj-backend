package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Confirmacao de persistencia de evento do runner com indicador de deduplicacao")
public record RunnerTaskEventResponse(
        @Schema(description = "Evento persistido ou recuperado por eventKey")
        TaskEventResponse event,
        @Schema(description = "Indica se o evento ja havia sido recebido anteriormente com a mesma eventKey",
                example = "false")
        boolean deduplicated
) {
}
