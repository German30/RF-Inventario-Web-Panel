package com.rf.panel.dto;

import com.rf.panel.entity.Ubicacion;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "Ubicacion")
public record UbicacionResponse(
        @Schema(example = "3") Long id,
        @Schema(example = "A-01-03") String codigo,
        @Schema(example = "Pasillo A, rack 1, nivel 3") String descripcion,
        @Schema(example = "Secos") String zona,
        boolean activa
) {
    public static UbicacionResponse from(Ubicacion u) {
        return new UbicacionResponse(u.getId(), u.getCodigo(), u.getDescripcion(), u.getZona(), u.isActiva());
    }
}
