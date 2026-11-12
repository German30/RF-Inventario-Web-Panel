package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(name = "ProductoNuevo", description = "Alta o edición de un ítem del inventario. "
        + "Los campos de ficha (descripción, categoría, unidad, mínimo, costo, lote, códigos) son "
        + "opcionales: si no vienen, en una edición se conservan.")
public record ProductoRequest(

        @Schema(example = "Router RF-2100", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 255) String nombre,

        @Schema(example = "RF2100", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 128) String sku,

        @Schema(description = "Stock total deseado. En un alta entra en la ubicación principal; en una "
                + "edición, la diferencia con el total actual se ajusta (positiva en la principal, "
                + "negativa FEFO).", example = "34", requiredMode = Schema.RequiredMode.REQUIRED)
        @PositiveOrZero int cantidad,

        @Schema(description = "Ubicación principal. Si no existe, se crea.", example = "A-03")
        @Size(max = 128) String ubicacion,

        @Schema(description = "`actualizadoEn` que leíste antes de editar. En una edición, si no "
                + "coincide con el valor actual del producto, se rechaza con 409: alguien más lo "
                + "guardó primero. Se ignora en un alta (no hay versión previa que defender).",
                example = "1756900000000")
        Long actualizadoEn,

        @Schema(example = "Router doble banda") @Size(max = 1000) String descripcion,
        @Schema(example = "Redes") @Size(max = 100) String categoria,
        @Schema(example = "UN") @Size(max = 20) String unidad,
        @Schema(example = "5") @PositiveOrZero Integer stockMinimo,
        @Schema(example = "45990.00") @PositiveOrZero BigDecimal costoUnitario,
        @Schema(description = "Si es true, toda entrada exige lote") Boolean controlaLote,
        @Schema(description = "Códigos de barras adicionales (EAN del proveedor, etc.)",
                example = "[\"7790001234567\"]") List<@Size(max = 128) String> codigosAlternativos
) {
    /** Forma corta (la de los clientes anteriores a V6). */
    public ProductoRequest(String nombre, String sku, int cantidad, String ubicacion, Long actualizadoEn) {
        this(nombre, sku, cantidad, ubicacion, actualizadoEn, null, null, null, null, null, null, null);
    }
}
