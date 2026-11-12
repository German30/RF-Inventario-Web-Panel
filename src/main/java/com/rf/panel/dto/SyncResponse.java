package com.rf.panel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "Sync", description = "Cambios desde el último cursor, para el caché offline de la app")
public record SyncResponse(
        @Schema(description = "Productos creados o modificados desde `desde`") List<ProductoResponse> productos,
        @Schema(description = "Ids de productos borrados desde `desde`") List<Long> eliminados,
        @Schema(description = "Todas las ubicaciones (son pocas)") List<UbicacionResponse> ubicaciones,
        @Schema(description = "Mandar como `desde` en la próxima sync. Se solapa un poco a propósito "
                + "para no perder transacciones que confirmaron tarde.") long cursor,
        @Schema(description = "true si `desde` era 0: el cliente debe reemplazar su caché, no fusionar")
        boolean completa
) {
}
