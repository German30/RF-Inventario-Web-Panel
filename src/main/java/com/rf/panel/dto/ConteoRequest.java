package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Cuerpos de las peticiones de conteo cíclico. */
public final class ConteoRequest {

    private ConteoRequest() {
    }

    @Schema(name = "ConteoNuevo", description = "Abrir una sesión de conteo")
    public record Abrir(
            @Schema(description = "Ubicación a contar. Vacío = conteo libre (solo ajusta lo contado).",
                    example = "A-01-03") @Size(max = 128) String ubicacion,
            @Schema(description = "Conteo ciego (default true): el operario no ve lo esperado")
            Boolean ciego,
            @Schema(example = "Conteo mensual pasillo A") @Size(max = 500) String nota
    ) {
    }

    @Schema(name = "ConteoLectura", description = "Una lectura del operario dentro de un conteo")
    public record Linea(
            @Schema(description = "SKU o código escaneado", example = "7790001234567",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 128) String codigo,
            @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull @PositiveOrZero Integer cantidad,
            @Schema(description = "Solo en conteos libres; si no viene, la principal del producto",
                    example = "A-01-03") @Size(max = 128) String ubicacion,
            @Schema(example = "L2026-11") @Size(max = 64) String lote,
            @Schema(description = "true (default) suma a lo ya contado; false lo reemplaza")
            Boolean sumar,
            @Size(max = 64) String clientOpId
    ) {
    }
}
