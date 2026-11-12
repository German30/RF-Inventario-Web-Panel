package com.rf.panel.dto;

import com.rf.panel.entity.Producto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(name = "Producto", description = "Ítem del inventario")
public record ProductoResponse(

        @Schema(example = "12") Long id,
        @Schema(example = "Router RF-2100") String nombre,
        @Schema(example = "RF2100") String sku,
        @Schema(description = "Stock total (todas las ubicaciones y lotes)", example = "34") int cantidad,
        @Schema(description = "Ubicación principal", example = "A-03") String ubicacion,

        @Schema(description = "Última modificación (epoch millis)", example = "1756900000000")
        long actualizadoEn,

        @Schema(description = "Última modificación en ISO-8601", example = "2026-09-03T15:20:00Z")
        Instant actualizadoIso,

        @Schema(example = "Router doble banda") String descripcion,
        @Schema(example = "Redes") String categoria,
        @Schema(example = "UN") String unidad,
        @Schema(example = "5") int stockMinimo,
        @Schema(example = "45990.00") BigDecimal costoUnitario,
        boolean controlaLote,
        List<String> codigosAlternativos,
        @Schema(description = "true si tiene mínimo configurado y el stock está en o por debajo")
        boolean bajoMinimo
) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(
                p.getId(),
                p.getNombre(),
                p.getSku(),
                p.getCantidad(),
                p.getUbicacion(),
                p.getActualizadoEn(),
                Instant.ofEpochMilli(p.getActualizadoEn()),
                p.getDescripcion(),
                p.getCategoria(),
                p.getUnidad(),
                p.getStockMinimo(),
                p.getCostoUnitario(),
                p.isControlaLote(),
                List.copyOf(p.getCodigosAlternativos()),
                p.bajoMinimo()
        );
    }
}
