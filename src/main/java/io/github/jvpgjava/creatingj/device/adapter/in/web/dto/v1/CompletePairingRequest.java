package io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload enviado pelo agente local para concluir o pareamento com codigo de uso unico")
public record CompletePairingRequest(
        @Schema(description = "Codigo de pareamento temporario exibido ao usuario ao criar o desafio",
                example = "crj_pair_9f4c2a1b8e7d6c5a4b3c2d1e0f9a8b7c")
        @NotBlank(message = "Codigo de pareamento obrigatorio")
        @Size(max = 200, message = "Codigo de pareamento invalido")
        String pairingCode
) {
}
