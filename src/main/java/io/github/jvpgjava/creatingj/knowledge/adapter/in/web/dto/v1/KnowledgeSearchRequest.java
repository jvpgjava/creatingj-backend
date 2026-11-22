package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Parametros para busca semantica RAG no indice vetorial do projeto")
public record KnowledgeSearchRequest(
        @Schema(description = "Consulta em linguagem natural para busca semantica",
                example = "Como funciona a protecao CSRF e cookie de sessao no login web?")
        @NotBlank(message = "Consulta de busca obrigatoria")
        @Size(max = 2000, message = "Consulta de busca deve ter no maximo 2000 caracteres")
        String query,
        @Schema(description = "Quantidade maxima de trechos retornados (padrao 5, intervalo 1..20)", example = "5")
        @Min(value = 1, message = "topK minimo e 1")
        @Max(value = 20, message = "topK maximo e 20")
        Integer topK,
        @Schema(description = "Pontuacao minima de similaridade de cosseno (-1.0 a 1.0)", example = "0.35")
        @DecimalMin(value = "-1.0", message = "minScore minimo e -1.0")
        @DecimalMax(value = "1.0", message = "minScore maximo e 1.0")
        Double minScore
) {
}
