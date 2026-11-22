package io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para envio de uma mensagem de usuario na conversa")
public record SendMessageRequest(
        @Schema(description = "Texto da mensagem a ser enviada ao modelo",
                example = "Quais camadas fazem parte do monólito modular hexagonal do CreatingJ?")
        @NotBlank @Size(max = 16000) String content
) {
}
