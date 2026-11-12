package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "AjusteStock", description = "Suma (o resta, con signo negativo) al stock actual")
public record AjusteStockRequest(

        @Schema(example = "-3", description = "Cantidad a sumar; negativo para descontar",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Integer delta
) {
}
