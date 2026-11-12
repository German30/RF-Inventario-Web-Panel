package com.rf.panel.dto;

import com.rf.panel.entity.Operario;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Operario", description = "Cuenta de operario. Nunca lleva el PIN ni su hash.")
public record OperarioResponse(

        @Schema(example = "1002") String codigo,
        @Schema(example = "Juan Pérez") String nombre,
        @Schema(example = "OPERARIO") String rol,
        @Schema(example = "true") boolean activo
) {
    public static OperarioResponse from(Operario o) {
        return new OperarioResponse(o.getCodigo(), o.getNombre(), o.getRol().name(), o.isActivo());
    }
}
