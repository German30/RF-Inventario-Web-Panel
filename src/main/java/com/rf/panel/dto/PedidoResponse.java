package com.rf.panel.dto;

import com.rf.panel.entity.Pedido;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

@Schema(name = "Pedido")
public record PedidoResponse(
        Long id,
        @Schema(description = "COMPRA | VENTA") String tipo,
        String numero,
        String contraparte,
        @Schema(description = "PENDIENTE | EN_PROCESO | COMPLETADO | CANCELADO") String estado,
        String nota,
        String creadoPor,
        long creadoEn,
        Long completadoEn,
        int unidadesPedidas,
        int unidadesHechas,
        List<Linea> lineas
) {

    @Schema(name = "PedidoLinea")
    public record Linea(
            Long id,
            Long productoId,
            String sku,
            String nombre,
            int pedida,
            int hecha,
            int pendiente,
            @Schema(description = "Solo VENTA: de dónde sacar (FEFO), para guiar el picking")
            List<ExistenciaResponse> sugerencias
    ) {
    }

    /** @param sugerencias por id de línea; vacío si no aplica o no se pidió detalle. */
    public static PedidoResponse from(Pedido p, Map<Long, List<ExistenciaResponse>> sugerencias, boolean incluirLineas) {
        int pedidas = p.getLineas().stream().mapToInt(l -> l.getPedido()).sum();
        int hechas = p.getLineas().stream().mapToInt(l -> l.getHecho()).sum();
        List<Linea> lineas = incluirLineas
                ? p.getLineas().stream().map(l -> new Linea(l.getId(), l.getProductoId(), l.getSku(), l.getNombre(),
                        l.getPedido(), l.getHecho(), l.pendiente(),
                        sugerencias.getOrDefault(l.getId(), List.of()))).toList()
                : List.of();
        return new PedidoResponse(p.getId(), p.getTipo().name(), p.getNumero(), p.getContraparte(),
                p.getEstado().name(), p.getNota(), p.getCreadoPor(), p.getCreadoEn(), p.getCompletadoEn(),
                pedidas, hechas, lineas);
    }
}
