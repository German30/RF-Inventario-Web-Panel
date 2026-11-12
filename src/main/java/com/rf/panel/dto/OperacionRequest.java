package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(name = "Operacion", description = """
        Movimiento de stock. El producto se indica por `productoId` o por `codigo`
        (SKU o código alternativo, tal cual se escaneó).
        - ENTRADA: suma `cantidad` en `ubicacion` (o la principal), con `lote`/`vencimiento` opcionales.
        - SALIDA: descuenta `cantidad` de `ubicacion`/`lote` si se indican; si no, FEFO.
        - TRANSFERENCIA: mueve `cantidad` de `ubicacion` (o la principal) a `ubicacionDestino`.
        - AJUSTE: `cantidad` con signo (negativo descuenta).
        """)
public record OperacionRequest(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "ENTRADA")
        @NotNull Tipo tipo,

        @Schema(example = "12") Long productoId,
        @Schema(example = "7790001234567") @Size(max = 128) String codigo,

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
        @NotNull Integer cantidad,

        @Schema(example = "A-01-03") @Size(max = 128) String ubicacion,
        @Schema(example = "B-02-01") @Size(max = 128) String ubicacionDestino,
        @Schema(example = "L2026-11") @Size(max = 64) String lote,
        @Schema(example = "2027-03-31") LocalDate vencimiento,
        @Schema(example = "Rotura") @Size(max = 255) String motivo,
        @Schema(example = "REM-0001-00012345") @Size(max = 100) String referencia,

        @Schema(description = "UUID que genera la app por operación. Reenviar el mismo en cada reintento: "
                + "si ya se aplicó, responde `repetida=true` sin volver a mover stock.",
                example = "6f1c1a2e-0b7b-4c55-9d1e-4b8e3c7f9a10")
        @Size(max = 64) String clientOpId
) {
    public enum Tipo { ENTRADA, SALIDA, TRANSFERENCIA, AJUSTE }
}
