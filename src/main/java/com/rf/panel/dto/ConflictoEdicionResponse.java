package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ConflictoEdicion", description = "Alguien más guardó este producto mientras lo editabas")
public record ConflictoEdicionResponse(

        @Schema(example = "Otro cambio se guardó mientras editabas RF2100.")
        String message,

        @Schema(description = "Estado actual del producto en el servidor")
        ProductoResponse actual
) {
}
