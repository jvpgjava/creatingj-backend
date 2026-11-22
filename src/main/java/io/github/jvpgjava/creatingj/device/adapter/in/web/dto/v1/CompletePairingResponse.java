package io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta do pareamento concluido com a credencial exclusiva do runner (exibida uma vez)")
public record CompletePairingResponse(
        @Schema(description = "Dados do dispositivo recem-pareado")
        DeviceSummaryResponse device,
        @Schema(description = "Token opaco X-Device-Token exclusivo para rotas /api/v1/runner/**",
                example = "crj_dev_7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2d")
        String deviceToken
) {
}
