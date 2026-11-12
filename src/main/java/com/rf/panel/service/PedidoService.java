package com.rf.panel.service;

import com.rf.panel.dto.ExistenciaResponse;
import com.rf.panel.dto.PedidoRequest;
import com.rf.panel.dto.PedidoResponse;
import com.rf.panel.entity.AccionMovimiento;
import com.rf.panel.entity.Pedido;
import com.rf.panel.entity.PedidoLinea;
import com.rf.panel.entity.Producto;
import com.rf.panel.entity.Ubicacion;
import com.rf.panel.exception.RecursoNoEncontradoException;
import com.rf.panel.exception.ReglaNegocioException;
import com.rf.panel.repository.ExistenciaRepository;
import com.rf.panel.repository.PedidoRepository;
import com.rf.panel.security.Sesion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recepción (pedidos de COMPRA: el escaneo suma stock) y picking (pedidos de
 * VENTA: el escaneo descuenta, por defecto FEFO). Cada escaneo es un
 * movimiento RECEPCION/PICKING con el número de pedido como referencia.
 */
@Service
@Transactional(readOnly = true)
public class PedidoService {

    private static final EnumSet<Pedido.Estado> ABIERTOS = EnumSet.of(Pedido.Estado.PENDIENTE, Pedido.Estado.EN_PROCESO);

    private final PedidoRepository repository;
    private final ExistenciaRepository existencias;
    private final InventarioService inventario;

    public PedidoService(PedidoRepository repository, ExistenciaRepository existencias, InventarioService inventario) {
        this.repository = repository;
        this.existencias = existencias;
        this.inventario = inventario;
    }

    public List<PedidoResponse> listar(Pedido.Tipo tipo, boolean soloAbiertos) {
        List<Pedido> pedidos;
        if (soloAbiertos) {
            pedidos = tipo == null
                    ? repository.findByEstadoInOrderByCreadoEnAsc(ABIERTOS)
                    : repository.findByTipoAndEstadoInOrderByCreadoEnAsc(tipo, ABIERTOS);
        } else {
            pedidos = repository.findTop100ByOrderByCreadoEnDesc().stream()
                    .filter(p -> tipo == null || p.getTipo() == tipo).toList();
        }
        return pedidos.stream().map(p -> PedidoResponse.from(p, Map.of(), false)).toList();
    }

    public PedidoResponse obtener(long id) {
        return detalle(buscar(id));
    }

    @Transactional
    public PedidoResponse crear(PedidoRequest.Crear req, Sesion sesion) {
        if (repository.existsByNumeroIgnoreCase(req.numero().trim())) {
            throw new ReglaNegocioException("Ya existe el pedido " + req.numero().trim() + ".");
        }
        Pedido pedido = new Pedido(req.tipo(), req.numero(), req.contraparte(), req.nota(), sesion.codigo());
        // Mismo producto en dos renglones: se suman en una línea.
        Map<Long, int[]> cantidades = new LinkedHashMap<>();
        Map<Long, Producto> productos = new HashMap<>();
        for (PedidoRequest.Linea l : req.lineas()) {
            Producto p = inventario.porCodigo(l.codigo());
            productos.put(p.getId(), p);
            cantidades.computeIfAbsent(p.getId(), k -> new int[1])[0] += l.cantidad();
        }
        cantidades.forEach((id, cant) -> {
            Producto p = productos.get(id);
            pedido.agregarLinea(new PedidoLinea(p.getId(), p.getSku(), p.getNombre(), cant[0]));
        });
        return detalle(repository.save(pedido));
    }

    @Transactional
    public PedidoResponse procesar(long id, PedidoRequest.Escaneo req, Sesion sesion) {
        Pedido pedido = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un pedido con id " + id + "."));
        if (inventario.yaProcesada(req.clientOpId())) {
            return detalle(pedido);
        }
        if (!pedido.abierto()) {
            throw new ReglaNegocioException("El pedido " + pedido.getNumero() + " está "
                    + pedido.getEstado().name().toLowerCase() + ".");
        }
        Producto encontrado = inventario.porCodigo(req.codigo());
        PedidoLinea linea = pedido.getLineas().stream()
                .filter(l -> l.getProductoId().equals(encontrado.getId()))
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(encontrado.getSku() + " no está en el pedido "
                        + pedido.getNumero() + "."));
        if (req.cantidad() > linea.pendiente()) {
            throw new ReglaNegocioException("Del " + linea.getSku() + " faltan " + linea.pendiente()
                    + "; no se pueden " + (pedido.getTipo() == Pedido.Tipo.COMPRA ? "recibir " : "preparar ")
                    + req.cantidad() + ".");
        }

        Producto p = inventario.bloquear(encontrado.getId());
        if (pedido.getTipo() == Pedido.Tipo.COMPRA) {
            Ubicacion u = inventario.ubicacionODefecto(req.ubicacion(), p);
            String lote = req.lote() == null ? "" : req.lote().trim();
            if (p.isControlaLote() && lote.isEmpty()) {
                throw new ReglaNegocioException(p.getSku() + " se controla por lote: indicá el lote.");
            }
            inventario.ingresar(p, u, lote, req.vencimiento(), req.cantidad());
            inventario.registrar(p, sesion, AccionMovimiento.RECEPCION, req.cantidad(), u.getCodigo(), "", lote,
                    pedido.getContraparte(), pedido.getNumero());
        } else {
            Ubicacion u = req.ubicacion() == null || req.ubicacion().isBlank() ? null : inventario.ubicacion(req.ubicacion());
            for (InventarioService.Parte parte : inventario.retirar(p, u, req.lote(), req.cantidad())) {
                inventario.registrar(p, sesion, AccionMovimiento.PICKING, -parte.cantidad(),
                        parte.ubicacion().getCodigo(), "", parte.lote(), pedido.getContraparte(), pedido.getNumero());
            }
        }
        linea.avanzar(req.cantidad());
        pedido.marcarEnProceso();
        inventario.marcarProcesada(req.clientOpId(), sesion);
        return detalle(pedido);
    }

    /** Cierra el pedido aunque queden pendientes (entrega parcial). */
    @Transactional
    public PedidoResponse completar(long id) {
        Pedido pedido = buscar(id);
        if (!pedido.abierto()) {
            throw new ReglaNegocioException("El pedido " + pedido.getNumero() + " ya está cerrado.");
        }
        pedido.completar();
        return detalle(pedido);
    }

    @Transactional
    public PedidoResponse cancelar(long id) {
        Pedido pedido = buscar(id);
        if (!pedido.abierto()) {
            throw new ReglaNegocioException("El pedido " + pedido.getNumero() + " ya está cerrado.");
        }
        if (pedido.getLineas().stream().anyMatch(l -> l.getHecho() > 0)) {
            throw new ReglaNegocioException("El pedido ya tiene movimientos: completalo como parcial en vez de cancelarlo.");
        }
        pedido.cancelar();
        return detalle(pedido);
    }

    /** Para picking: de dónde sacar cada línea, en orden FEFO (hasta 3 sugerencias). */
    private PedidoResponse detalle(Pedido pedido) {
        Map<Long, List<ExistenciaResponse>> sugerencias = new HashMap<>();
        if (pedido.getTipo() == Pedido.Tipo.VENTA && pedido.abierto()) {
            for (PedidoLinea l : pedido.getLineas()) {
                if (l.pendiente() == 0) continue;
                Producto p = inventario.resolver(l.getProductoId(), null);
                sugerencias.put(l.getId(), existencias.paraDescontar(p.getId(), null, null).stream()
                        .limit(3).map(e -> ExistenciaResponse.from(e, p)).toList());
            }
        }
        return PedidoResponse.from(pedido, sugerencias, true);
    }

    private Pedido buscar(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un pedido con id " + id + "."));
    }
}
