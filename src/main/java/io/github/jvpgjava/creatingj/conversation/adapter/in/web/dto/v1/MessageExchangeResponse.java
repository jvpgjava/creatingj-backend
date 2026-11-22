package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resultado sincrono do envio de mensagem com resposta, geracao, follow-ups e trace de guardrails")
public record MessageExchangeResponse(
        @Schema(description = "Mensagem persistida do usuario")
        MessageResponse userMessage,
        @Schema(description = "Resposta persistida do assistente")
        MessageResponse assistantMessage,
        @Schema(description = "Metadados e contagem de tokens da geracao")
        GenerationSummaryResponse generation,
        @Schema(description = "Ate 3 sugestoes de perguntas de continuacao dentro do escopo tecnico",
                example = "[\"Como testar esse fluxo de autenticacao com MockMvc?\"]")
        List<String> followUpSuggestions,
        @Schema(description = "Flags de auditoria acionadas pelos guardrails de entrada ou saida (vazio quando limpo)",
                example = "[]")
        List<String> guardrailFlags
) {
}
