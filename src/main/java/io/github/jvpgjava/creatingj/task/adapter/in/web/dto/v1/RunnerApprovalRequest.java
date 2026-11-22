package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RunnerApprovalRequest(
        @NotBlank(message = "Nome da ferramenta obrigatorio")
        @Size(max = 40, message = "Nome da ferramenta invalido")
        String toolName,
        @Size(max = 260, message = "Caminho alvo excede o limite")
        String targetPath,
        @Size(max = 1000, message = "Comando excede o limite")
        String commandText,
        @Size(max = 8000, message = "Diff preview excede o limite")
        String diffPreview,
        @Min(value = 30, message = "TTL minimo e 30 segundos")
        @Max(value = 600, message = "TTL maximo e 600 segundos")
        Integer ttlSeconds
) {
}
