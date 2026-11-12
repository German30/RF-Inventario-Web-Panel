package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "ErrorCliente", description = "Crash reportado por la app (se loguea en el servidor)")
public record ErrorClienteRequest(
        @NotBlank @Size(max = 500) String mensaje,
        @Size(max = 20000) String stack,
        @Size(max = 50) String version,
        @Size(max = 200) String dispositivo,
        Long ocurridoEn
) {
}
