package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para criacao de uma nova conversa de chat")
public record CreateConversationRequest(
        @Schema(description = "Titulo inicial da conversa", example = "Revisao de seguranca do backend")
        @NotBlank @Size(max = 160) String title,
        @Schema(description = "Modelo opcional (quando omitido, utiliza o modelo padrao do provedor)",
                example = "z-ai/glm-5.3-flash")
        @Size(max = 120) String modelId
) {
}
