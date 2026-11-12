package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "Login", description = "Credenciales del operario: las mismas de la app Android")
public record LoginRequest(

        @Schema(example = "1001", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Pattern(regexp = "\\d{1,12}", message = "el código es numérico")
        String codigo,

        @Schema(example = "1234", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Pattern(regexp = "\\d{3,10}", message = "el PIN es numérico (3 a 10 dígitos)")
        String pin
) {
}
