package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "ResetPin", description = "Nuevo PIN para un operario")
public record ResetPinRequest(

        @Schema(example = "9999", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "\\d{3,10}", message = "el PIN es numérico (3 a 10 dígitos)")
        String pin
) {
}
