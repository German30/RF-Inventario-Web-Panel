package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/** Forma única de los errores de la API. La arma {@code GlobalExceptionHandler}. */
@Schema(name = "Error", description = "Respuesta de error de la API")
public record ErrorResponse(

        @Schema(example = "2026-09-03T17:02:05.287Z")
        Instant timestamp,

        @Schema(example = "409")
        int status,

        @Schema(example = "Conflict")
        String error,

        @Schema(example = "Ya existe un producto con el SKU RF2100.")
        String message,

        @Schema(description = "Solo en errores de validación: qué campo falló y por qué",
                example = "{\"pin\": \"el PIN es numérico (3 a 10 dígitos)\"}")
        Map<String, String> campos
) {
    public static ErrorResponse de(int status, String error, String mensaje) {
        return new ErrorResponse(Instant.now(), status, error, mensaje, null);
    }

    public ErrorResponse conCampos(Map<String, String> campos) {
        return new ErrorResponse(timestamp(), status(), error(), message(), campos);
    }
}
