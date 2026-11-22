package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeSearchRequest(
        @NotBlank(message = "Consulta de busca obrigatoria")
        @Size(max = 2000, message = "Consulta de busca deve ter no maximo 2000 caracteres")
        String query,
        @Min(value = 1, message = "topK minimo e 1")
        @Max(value = 20, message = "topK maximo e 20")
        Integer topK,
        @DecimalMin(value = "-1.0", message = "minScore minimo e -1.0")
        @DecimalMax(value = "1.0", message = "minScore maximo e 1.0")
        Double minScore
) {
}
