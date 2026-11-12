package com.rf.panel.dto;

import com.rf.panel.entity.RolOperario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(name = "OperarioNuevo", description = "Alta de un operario")
public record OperarioRequest(

        @Schema(example = "1002", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "\\d{1,12}", message = "el código es numérico")
        String codigo,

        @Schema(example = "5678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "\\d{3,10}", message = "el PIN es numérico (3 a 10 dígitos)")
        String pin,

        @Schema(example = "Juan Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String nombre,

        @Schema(example = "OPERARIO", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull RolOperario rol
) {
}
