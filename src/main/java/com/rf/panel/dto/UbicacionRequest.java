package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "UbicacionNueva", description = "Alta o edición de una ubicación")
public record UbicacionRequest(
        @Schema(example = "A-01-03", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 128)
        @Pattern(regexp = "[A-Za-z0-9._/-]+", message = "solo letras, números y . _ / -")
        String codigo,

        @Schema(example = "Pasillo A, rack 1, nivel 3") @Size(max = 255) String descripcion,
        @Schema(example = "Secos") @Size(max = 64) String zona,
        @Schema(description = "null = activa") Boolean activa
) {
}
