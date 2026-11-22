package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para ingestao ou atualizacao de uma fonte de conhecimento do projeto")
public record IngestKnowledgeSourceRequest(
        @Schema(description = "Caminho relativo seguro dentro do projeto (arquivos sensiveis como .env sao bloqueados)",
                example = "docs/arquitetura-auth.md")
        @NotBlank(message = "Caminho da fonte obrigatorio")
        @Size(max = 260, message = "Caminho da fonte deve ter no maximo 260 caracteres")
        String sourcePath,
        @Schema(description = "Titulo descritivo da fonte", example = "Arquitetura de Autenticacao")
        @Size(max = 160, message = "Titulo deve ter no maximo 160 caracteres")
        String title,
        @Schema(description = "Conteudo textual a ser dividido em chunks e indexado no pgvector",
                example = "A autenticacao web utiliza cookie HttpOnly CREATINGJ_SESSION com CSRF X-XSRF-TOKEN.")
        @NotBlank(message = "Conteudo da fonte obrigatorio")
        @Size(max = 100000, message = "Conteudo excede o limite de 100000 caracteres")
        String content
) {
}
