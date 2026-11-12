package com.rf.panel.service;

import com.rf.panel.dto.DashboardResponse;
import com.rf.panel.dto.ExistenciaResponse;
import com.rf.panel.dto.ProductoResponse;
import com.rf.panel.entity.Conteo;
import com.rf.panel.entity.Existencia;
import com.rf.panel.entity.Pedido;
import com.rf.panel.entity.Producto;
import com.rf.panel.repository.ConteoRepository;
import com.rf.panel.repository.ExistenciaRepository;
import com.rf.panel.repository.MovimientoStockRepository;
import com.rf.panel.repository.PedidoRepository;
import com.rf.panel.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int DIAS_ALERTA_VENCIMIENTO = 30;
    private static final EnumSet<Pedido.Estado> ABIERTOS = EnumSet.of(Pedido.Estado.PENDIENTE, Pedido.Estado.EN_PROCESO);

    private final ProductoRepository productos;
    private final ExistenciaRepository existencias;
    private final MovimientoStockRepository movimientos;
    private final ConteoRepository conteos;
    private final PedidoRepository pedidos;

    public DashboardService(ProductoRepository productos, ExistenciaRepository existencias,
                            MovimientoStockRepository movimientos, ConteoRepository conteos,
                            PedidoRepository pedidos) {
        this.productos = productos;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.conteos = conteos;
        this.pedidos = pedidos;
    }

    public DashboardResponse resumen() {
        long inicioDelDia = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        List<Producto> bajoMinimo = productos.bajoMinimo();
        List<Existencia> porVencer = existencias.porVencer(LocalDate.now().plusDays(DIAS_ALERTA_VENCIMIENTO));
        Map<Long, Producto> porId = productos.findAllById(porVencer.stream().map(Existencia::getProductoId).toList())
                .stream().collect(Collectors.toMap(Producto::getId, Function.identity()));
        BigDecimal valor = productos.valorTotal();

        return new DashboardResponse(
                productos.count(),
                productos.unidadesTotales(),
                valor == null ? BigDecimal.ZERO : valor,
                productos.countByCantidad(0),
                bajoMinimo.size(),
                movimientos.countByCreadoEnGreaterThanEqual(inicioDelDia),
                conteos.findByEstadoInOrderByCreadoEnDesc(EnumSet.of(Conteo.Estado.ABIERTO, Conteo.Estado.CERRADO)).size(),
                pedidos.findByTipoAndEstadoInOrderByCreadoEnAsc(Pedido.Tipo.COMPRA, ABIERTOS).size(),
                pedidos.findByTipoAndEstadoInOrderByCreadoEnAsc(Pedido.Tipo.VENTA, ABIERTOS).size(),
                bajoMinimo.stream().limit(20).map(ProductoResponse::from).toList(),
                porVencer.stream().limit(20).map(e -> ExistenciaResponse.from(e, porId.get(e.getProductoId()))).toList()
        );
    }
}
