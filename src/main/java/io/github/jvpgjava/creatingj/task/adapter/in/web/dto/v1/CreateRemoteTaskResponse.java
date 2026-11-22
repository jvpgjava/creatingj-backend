package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta da criacao de tarefa remota indicando presenca do dispositivo e deduplicacao")
public record CreateRemoteTaskResponse(
        @Schema(description = "Resumo da tarefa criada ou deduplicada")
        RemoteTaskSummaryResponse task,
        @Schema(description = "Indica se o dispositivo de destino estava online no momento do enfileiramento",
                example = "true")
        boolean deviceOnline,
        @Schema(description = "Indica se a requisicao reutilizou uma tarefa existente pela idempotencyKey",
                example = "false")
        boolean deduplicated
) {
}
