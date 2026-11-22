package io.github.jvpgjava.creatingj.agent.adapter.in.web.dto.v1;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkspaceFileSnapshotRequest(
        @NotBlank(message = "Caminho relativo do arquivo obrigatorio")
        @Size(max = 260, message = "Caminho do arquivo deve ter no maximo 260 caracteres")
        String relativePath,
        @NotBlank(message = "Conteudo do arquivo obrigatorio")
        @Size(max = 12000, message = "Conteudo do arquivo deve ter no maximo 12000 caracteres")
        String content
) {
}
