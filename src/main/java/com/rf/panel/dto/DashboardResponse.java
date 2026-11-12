package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(name = "Dashboard", description = "Resumen del inventario para la pantalla de inicio")
public record DashboardResponse(
        long productos,
        long unidades,
        @Schema(description = "Σ cantidad × costo unitario") BigDecimal valorInventario,
        long sinStock,
        long bajoMinimo,
        long movimientosHoy,
        long conteosAbiertos,
        long recepcionesPendientes,
        long pickingsPendientes,
        @Schema(description = "Hasta 20, el de menos stock primero") List<ProductoResponse> alertasBajoMinimo,
        @Schema(description = "Lotes que vencen en los próximos 30 días (o ya vencidos), hasta 20")
        List<ExistenciaResponse> proximosAVencer
) {
}
