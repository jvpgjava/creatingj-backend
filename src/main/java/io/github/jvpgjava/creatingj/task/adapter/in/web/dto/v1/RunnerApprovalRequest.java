package io.github.jvpgjava.creatingj.task.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Pedido enviado pelo runner para obter aprovacao humana temporaria antes de executar ferramenta")
public record RunnerApprovalRequest(
        @Schema(description = "Nome da ferramenta que o runner deseja executar", example = "run_command")
        @NotBlank(message = "Nome da ferramenta obrigatorio")
        @Size(max = 40, message = "Nome da ferramenta invalido")
        String toolName,
        @Schema(description = "Caminho relativo alvo no workspace (quando aplicavel)",
                example = "src/main/java/io/github/jvpgjava/creatingj/bootstrap/config/OpenApiConfig.java")
        @Size(max = 260, message = "Caminho alvo excede o limite")
        String targetPath,
        @Schema(description = "Comando proposto para execucao local", example = "mvn -B test")
        @Size(max = 1000, message = "Comando excede o limite")
        String commandText,
        @Schema(description = "Pre-visualizacao do diff ou impacto esperado",
                example = "--- a/pom.xml\n+++ b/pom.xml\n@@ -10,1 +10,1 @@")
        @Size(max = 8000, message = "Diff preview excede o limite")
        String diffPreview,
        @Schema(description = "Tempo de expiracao do pedido em segundos (30..600)", example = "180")
        @Min(value = 30, message = "TTL minimo e 30 segundos")
        @Max(value = 600, message = "TTL maximo e 600 segundos")
        Integer ttlSeconds
) {
}
