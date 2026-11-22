package io.github.jvpgjava.creatingj.device.adapter.in.web.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Lista de dispositivos pareados do usuario autenticado")
public record DevicesResponse(
        @Schema(description = "Dispositivos ordenados do mais recente ao mais antigo")
        List<DeviceSummaryResponse> items
) {
}
