package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Snapshot textual de um arquivo do workspace enviado pelo cliente como contexto")
public record WorkspaceFileSnapshotRequest(
        @Schema(description = "Caminho relativo seguro dentro do repositorio",
                example = "src/main/java/io/github/jvpgjava/creatingj/bootstrap/config/security/CorsProperties.java")
        @NotBlank(message = "Caminho relativo do arquivo obrigatorio")
        @Size(max = 260, message = "Caminho do arquivo deve ter no maximo 260 caracteres")
        String relativePath,
        @Schema(description = "Conteudo textual do arquivo (sem segredos ou credenciais)",
                example = "public record CorsProperties(List<String> allowedOrigins) {}")
        @NotBlank(message = "Conteudo do arquivo obrigatorio")
        @Size(max = 12000, message = "Conteudo do arquivo deve ter no maximo 12000 caracteres")
        String content
) {
}
