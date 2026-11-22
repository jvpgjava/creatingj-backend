package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IngestKnowledgeSourceRequest(
        @NotBlank(message = "Caminho da fonte obrigatorio")
        @Size(max = 260, message = "Caminho da fonte deve ter no maximo 260 caracteres")
        String sourcePath,
        @Size(max = 160, message = "Titulo deve ter no maximo 160 caracteres")
        String title,
        @NotBlank(message = "Conteudo da fonte obrigatorio")
        @Size(max = 100000, message = "Conteudo excede o limite de 100000 caracteres")
        String content
) {
}
