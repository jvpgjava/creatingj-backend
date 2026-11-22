package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado sincrono do envio de mensagem com a resposta do assistente e a geracao")
public record MessageExchangeResponse(
        @Schema(description = "Mensagem persistida do usuario")
        MessageResponse userMessage,
        @Schema(description = "Resposta persistida do assistente")
        MessageResponse assistantMessage,
        @Schema(description = "Metadados e contagem de tokens da geracao")
        GenerationSummaryResponse generation
) {
}
