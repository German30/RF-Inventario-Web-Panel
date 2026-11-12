package com.rf.panel.dto;

import com.rf.panel.entity.RolOperario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "OperarioEdicion", description = "Editar nombre, rol o si está activo (no el PIN, ver /pin)")
public record ActualizarOperarioRequest(

        @Schema(example = "Juan Pérez", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String nombre,

        @Schema(example = "SUPERVISOR", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull RolOperario rol,

        @Schema(description = "false = desactivado: no puede entrar ni a la app ni al panel",
                example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Boolean activo
) {
}
