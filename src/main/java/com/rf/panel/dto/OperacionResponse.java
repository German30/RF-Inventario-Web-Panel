package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ResultadoOperacion", description = "Estado del producto después de la operación y los movimientos que generó")
public record OperacionResponse(
        ProductoResponse producto,
        List<MovimientoStockResponse> movimientos,
        @Schema(description = "true si el clientOpId ya se había aplicado: no se movió nada esta vez")
        boolean repetida
) {
}
