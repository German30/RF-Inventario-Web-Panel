package com.rf.panel.dto;

import com.rf.panel.entity.MovimientoStock;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "MovimientoStock", description = "Una entrada del historial: quién tocó el producto, cuándo y qué cambió")
public record MovimientoStockResponse(

        @Schema(example = "1") Long id,
        @Schema(example = "RF2100") String sku,
        @Schema(example = "1001") String operadorCodigo,
        @Schema(example = "Operario Demo") String operadorNombre,

        @Schema(description = "CREACION | EDICION | AJUSTE_STOCK | ELIMINACION | ENTRADA | SALIDA | "
                + "TRANSFERENCIA | CONTEO | RECEPCION | PICKING", example = "AJUSTE_STOCK")
        String accion,

        @Schema(description = "Cambio de cantidad en este movimiento (puede ser negativo)", example = "-3")
        int delta,

        @Schema(description = "Cantidad total del producto después del movimiento", example = "17")
        int cantidadDespues,

        @Schema(example = "1756900000000") long creadoEn,
        @Schema(example = "2026-09-04T15:20:00Z") Instant creadoIso,

        @Schema(example = "12") Long productoId,
        @Schema(example = "Rotura") String motivo,
        @Schema(example = "OC-000123") String referencia,
        @Schema(example = "A-01-03") String ubicacion,
        @Schema(description = "En una transferencia: la otra punta", example = "B-02-01") String ubicacionDestino,
        @Schema(example = "L2026-11") String lote
) {
    public static MovimientoStockResponse from(MovimientoStock m) {
        return new MovimientoStockResponse(
                m.getId(),
                m.getSku(),
                m.getOperadorCodigo(),
                m.getOperadorNombre(),
                m.getAccion().name(),
                m.getDelta(),
                m.getCantidadDespues(),
                m.getCreadoEn(),
                Instant.ofEpochMilli(m.getCreadoEn()),
                m.getProductoId(),
                m.getMotivo(),
                m.getReferencia(),
                m.getUbicacion(),
                m.getUbicacionDestino(),
                m.getLote()
        );
    }
}
