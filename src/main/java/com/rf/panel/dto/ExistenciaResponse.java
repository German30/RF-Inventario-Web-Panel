package com.rf.panel.dto;

import com.rf.panel.entity.Existencia;
import com.rf.panel.entity.Producto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name = "Existencia", description = "Stock de un producto en una ubicación y lote")
public record ExistenciaResponse(
        @Schema(example = "12") Long productoId,
        @Schema(example = "RF2100") String sku,
        @Schema(example = "Router RF-2100") String nombre,
        @Schema(example = "A-01-03") String ubicacion,
        @Schema(description = "Vacío = sin lote", example = "L2026-11") String lote,
        @Schema(example = "2027-03-31") LocalDate vence,
        @Schema(example = "8") int cantidad
) {
    public static ExistenciaResponse from(Existencia e, Producto p) {
        return new ExistenciaResponse(e.getProductoId(), p == null ? "" : p.getSku(),
                p == null ? "" : p.getNombre(), e.getUbicacion().getCodigo(), e.getLote(), e.getVence(),
                e.getCantidad());
    }
}
