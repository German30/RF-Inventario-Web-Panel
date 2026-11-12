package com.rf.panel.dto;

import com.rf.panel.entity.Pedido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/** Cuerpos de las peticiones de pedidos (recepción y picking). */
public final class PedidoRequest {

    private PedidoRequest() {
    }

    @Schema(name = "PedidoNuevo", description = "COMPRA = a recibir; VENTA = a preparar (picking)")
    public record Crear(
            @Schema(example = "COMPRA", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull Pedido.Tipo tipo,
            @Schema(example = "OC-000123", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 64) String numero,
            @Schema(description = "Proveedor o cliente", example = "Distribuidora Norte")
            @Size(max = 255) String contraparte,
            @Size(max = 500) String nota,
            @NotEmpty @Valid List<Linea> lineas
    ) {
    }

    @Schema(name = "PedidoLineaNueva")
    public record Linea(
            @Schema(description = "SKU o código alternativo", example = "RF2100") @NotBlank String codigo,
            @Schema(example = "24") @NotNull @Positive Integer cantidad
    ) {
    }

    @Schema(name = "PedidoEscaneo", description = "El operario recibió o pickeó `cantidad` de un producto del pedido")
    public record Escaneo(
            @Schema(example = "7790001234567", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank @Size(max = 128) String codigo,
            @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull @Positive Integer cantidad,
            @Schema(description = "Compra: dónde se guarda (default: principal). Venta: de dónde se saca "
                    + "(default: FEFO).", example = "A-01-03") @Size(max = 128) String ubicacion,
            @Size(max = 64) String lote,
            @Schema(description = "Solo compras") LocalDate vencimiento,
            @Size(max = 64) String clientOpId
    ) {
    }
}
