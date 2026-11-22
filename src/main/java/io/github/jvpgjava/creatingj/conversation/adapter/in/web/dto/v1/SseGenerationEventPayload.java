package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload de eventos SSE enviados durante o streaming da geracao de resposta")
public record SseGenerationEventPayload(
        @Schema(description = "Identificador da geracao em andamento",
                example = "550e8400-e29b-41d4-a716-446655440021")
        String generationId,
        @Schema(description = "Status do evento (STARTED, DELTA, COMPLETED, CANCELLED, FAILED)", example = "DELTA")
        String status,
        @Schema(description = "Trecho incremental de texto emitido no evento DELTA",
                example = "A camada de dominio permanece livre de frameworks ")
        String delta,
        @Schema(description = "Identificador da mensagem final do assistente (preenchido em COMPLETED)",
                example = "550e8400-e29b-41d4-a716-446655440023", nullable = true)
        String assistantMessageId,
        @Schema(description = "Tokens de prompt (preenchido no encerramento)", example = "120", nullable = true)
        Integer promptTokens,
        @Schema(description = "Tokens de resposta (preenchido no encerramento)", example = "185", nullable = true)
        Integer completionTokens,
        @Schema(description = "Total de tokens (preenchido no encerramento)", example = "305", nullable = true)
        Integer totalTokens,
        @Schema(description = "Codigo de erro em caso de falha", nullable = true)
        String errorCode
) {
}
