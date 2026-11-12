package com.rf.panel.service;

import com.rf.panel.dto.OperacionRequest;
import com.rf.panel.dto.OperacionResponse;
import com.rf.panel.dto.MovimientoStockResponse;
import com.rf.panel.dto.ProductoResponse;
import com.rf.panel.entity.AccionMovimiento;
import com.rf.panel.entity.Existencia;
import com.rf.panel.entity.MovimientoStock;
import com.rf.panel.entity.OperacionProcesada;
import com.rf.panel.entity.Producto;
import com.rf.panel.entity.Ubicacion;
import com.rf.panel.exception.RecursoNoEncontradoException;
import com.rf.panel.exception.ReglaNegocioException;
import com.rf.panel.repository.ExistenciaRepository;
import com.rf.panel.repository.MovimientoStockRepository;
import com.rf.panel.repository.OperacionProcesadaRepository;
import com.rf.panel.repository.ProductoRepository;
import com.rf.panel.repository.UbicacionRepository;
import com.rf.panel.security.Sesion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Motor de stock. ÚNICO lugar que modifica {@link Existencia} y el total de
 * {@link Producto}; productos, conteos y pedidos delegan acá.
 *
 * Reglas:
 * <ul>
 *   <li>Cada operación bloquea la fila del producto (SELECT ... FOR UPDATE)
 *       antes de leer existencias, así dos operarios no pisan sus cambios.</li>
 *   <li>El stock nunca queda negativo en ninguna ubicación/lote.</li>
 *   <li>Las salidas sin lote/ubicación explícitos descuentan FEFO (primero lo
 *       que vence antes).</li>
 *   <li>Cada parte movida deja una fila en el historial.</li>
 *   <li>Con {@code clientOpId}, una operación repetida (reintento de la cola
 *       offline de la app) no vuelve a mover stock.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class InventarioService {

    private final ProductoRepository productos;
    private final ExistenciaRepository existencias;
    private final UbicacionRepository ubicaciones;
    private final MovimientoStockRepository movimientos;
    private final OperacionProcesadaRepository operaciones;

    public InventarioService(ProductoRepository productos, ExistenciaRepository existencias,
                             UbicacionRepository ubicaciones, MovimientoStockRepository movimientos,
                             OperacionProcesadaRepository operaciones) {
        this.productos = productos;
        this.existencias = existencias;
        this.ubicaciones = ubicaciones;
        this.movimientos = movimientos;
        this.operaciones = operaciones;
    }

    /** Una porción descontada de una existencia concreta. */
    public record Parte(Ubicacion ubicacion, String lote, LocalDate vence, int cantidad) {
    }

    // ------------------------------------------------------------------
    // Operación genérica (POST /api/operaciones)
    // ------------------------------------------------------------------

    @Transactional
    public OperacionResponse operar(OperacionRequest req, Sesion sesion) {
        Producto encontrado = resolver(req.productoId(), req.codigo());
        if (yaProcesada(req.clientOpId())) {
            return new OperacionResponse(ProductoResponse.from(encontrado), List.of(), true);
        }
        Producto p = bloquear(encontrado.getId());
        String motivo = req.motivo();
        String ref = req.referencia();
        List<MovimientoStock> hechos = new ArrayList<>();

        switch (req.tipo()) {
            case ENTRADA -> {
                exigirPositiva(req.cantidad());
                Ubicacion u = ubicacionODefecto(req.ubicacion(), p);
                String lote = loteValido(p, req.lote());
                ingresar(p, u, lote, req.vencimiento(), req.cantidad());
                hechos.add(registrar(p, sesion, AccionMovimiento.ENTRADA, req.cantidad(), u.getCodigo(), "",
                        lote, motivo, ref));
            }
            case SALIDA -> {
                exigirPositiva(req.cantidad());
                Ubicacion u = req.ubicacion() == null || req.ubicacion().isBlank() ? null : ubicacion(req.ubicacion());
                for (Parte parte : retirar(p, u, req.lote(), req.cantidad())) {
                    hechos.add(registrar(p, sesion, AccionMovimiento.SALIDA, -parte.cantidad(),
                            parte.ubicacion().getCodigo(), "", parte.lote(), motivo, ref));
                }
            }
            case TRANSFERENCIA -> {
                exigirPositiva(req.cantidad());
                Ubicacion origen = ubicacionODefecto(req.ubicacion(), p);
                if (req.ubicacionDestino() == null || req.ubicacionDestino().isBlank()) {
                    throw new ReglaNegocioException("Indicá la ubicación de destino.");
                }
                Ubicacion destino = ubicacion(req.ubicacionDestino());
                if (destino.getId().equals(origen.getId())) {
                    throw new ReglaNegocioException("El origen y el destino son la misma ubicación.");
                }
                for (Parte parte : retirar(p, origen, req.lote(), req.cantidad())) {
                    hechos.add(registrar(p, sesion, AccionMovimiento.TRANSFERENCIA, -parte.cantidad(),
                            origen.getCodigo(), destino.getCodigo(), parte.lote(), motivo, ref));
                    ingresar(p, destino, parte.lote(), parte.vence(), parte.cantidad());
                    hechos.add(registrar(p, sesion, AccionMovimiento.TRANSFERENCIA, parte.cantidad(),
                            destino.getCodigo(), origen.getCodigo(), parte.lote(), motivo, ref));
                }
            }
            case AJUSTE -> {
                int delta = req.cantidad() == null ? 0 : req.cantidad();
                if (delta == 0) throw new ReglaNegocioException("El ajuste no puede ser 0.");
                hechos.addAll(ajustar(p, sesion, delta, req.ubicacion(), req.lote(), req.vencimiento(), motivo, ref));
            }
        }

        marcarProcesada(req.clientOpId(), sesion);
        return new OperacionResponse(ProductoResponse.from(p),
                hechos.stream().map(MovimientoStockResponse::from).toList(), false);
    }

    /**
     * Ajuste con signo, reutilizado por el PATCH /stock legado: positivo entra
     * en la ubicación indicada (o la principal); negativo sale de esa ubicación
     * o, si no se indica, FEFO de cualquier ubicación.
     */
    @Transactional
    public List<MovimientoStock> ajustar(Producto p, Sesion sesion, int delta, String ubicacion, String lote,
                                         LocalDate vence, String motivo, String ref) {
        List<MovimientoStock> hechos = new ArrayList<>();
        if (delta > 0) {
            Ubicacion u = ubicacionODefecto(ubicacion, p);
            String l = lote == null ? "" : lote.trim();
            ingresar(p, u, l, vence, delta);
            hechos.add(registrar(p, sesion, AccionMovimiento.AJUSTE_STOCK, delta, u.getCodigo(), "", l, motivo, ref));
        } else if (delta < 0) {
            Ubicacion u = ubicacion == null || ubicacion.isBlank() ? null : ubicacion(ubicacion);
            for (Parte parte : retirar(p, u, lote, -delta)) {
                hechos.add(registrar(p, sesion, AccionMovimiento.AJUSTE_STOCK, -parte.cantidad(),
                        parte.ubicacion().getCodigo(), "", parte.lote(), motivo, ref));
            }
        }
        return hechos;
    }

    // ------------------------------------------------------------------
    // Primitivas (el producto tiene que venir bloqueado con bloquear())
    // ------------------------------------------------------------------

    public Producto bloquear(Long productoId) {
        return productos.findByIdForUpdate(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un producto con id " + productoId + "."));
    }

    /** Suma stock en una ubicación/lote y actualiza el total del producto. */
    @Transactional
    public void ingresar(Producto p, Ubicacion u, String lote, LocalDate vence, int cantidad) {
        if (!u.isActiva()) {
            throw new ReglaNegocioException("La ubicación " + u.getCodigo() + " está inactiva.");
        }
        String l = lote == null ? "" : lote.trim();
        Existencia e = existencias.buscar(p.getId(), u.getId(), l)
                .orElseGet(() -> existencias.save(new Existencia(p.getId(), u, l, vence)));
        e.fijarVencimiento(vence);
        e.sumar(cantidad);
        p.fijarCantidadTotal(p.getCantidad() + cantidad);
    }

    /**
     * Descuenta `cantidad` (FEFO dentro de lo que permitan `u` y `lote`, que
     * pueden ser null = cualquiera). Falla sin tocar nada si no alcanza.
     */
    @Transactional
    public List<Parte> retirar(Producto p, Ubicacion u, String lote, int cantidad) {
        String l = lote == null || lote.isBlank() ? null : lote.trim();
        List<Existencia> candidatas = existencias.paraDescontar(p.getId(), u == null ? null : u.getId(), l);
        int disponible = candidatas.stream().mapToInt(Existencia::getCantidad).sum();
        if (disponible < cantidad) {
            String donde = (u == null ? "" : " en " + u.getCodigo()) + (l == null ? "" : " (lote " + l + ")");
            throw new ReglaNegocioException("Stock insuficiente de " + p.getSku() + donde
                    + ": hay " + disponible + ", se pidieron " + cantidad + ".");
        }
        List<Parte> partes = new ArrayList<>();
        int falta = cantidad;
        for (Existencia e : candidatas) {
            if (falta == 0) break;
            int tomar = Math.min(falta, e.getCantidad());
            e.sumar(-tomar);
            partes.add(new Parte(e.getUbicacion(), e.getLote(), e.getVence(), tomar));
            falta -= tomar;
        }
        p.fijarCantidadTotal(p.getCantidad() - cantidad);
        return partes;
    }

    /** Lleva una ubicación/lote a una cantidad exacta (conteo). Devuelve el delta aplicado. */
    @Transactional
    public int fijarEn(Producto p, Ubicacion u, String lote, int cantidad) {
        String l = lote == null ? "" : lote.trim();
        Existencia e = existencias.buscar(p.getId(), u.getId(), l)
                .orElseGet(() -> existencias.save(new Existencia(p.getId(), u, l, null)));
        int delta = cantidad - e.getCantidad();
        if (delta != 0) {
            e.fijar(cantidad);
            p.fijarCantidadTotal(p.getCantidad() + delta);
        }
        return delta;
    }

    public int cantidadEn(Producto p, Ubicacion u, String lote) {
        return existencias.buscar(p.getId(), u.getId(), lote == null ? "" : lote.trim())
                .map(Existencia::getCantidad).orElse(0);
    }

    @Transactional
    public MovimientoStock registrar(Producto p, Sesion sesion, AccionMovimiento accion, int delta,
                                     String ubicacion, String destino, String lote, String motivo, String ref) {
        return movimientos.save(new MovimientoStock(
                p.getId(), p.getSku(), sesion.codigo(), sesion.nombre(), accion, delta, p.getCantidad())
                .conDetalle(motivo, ref, ubicacion, destino, lote));
    }

    // ------------------------------------------------------------------
    // Resolución de productos y ubicaciones
    // ------------------------------------------------------------------

    /** Por id, o por código escaneado (SKU o código alternativo). */
    public Producto resolver(Long productoId, String codigo) {
        if (productoId != null) {
            return productos.findById(productoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No hay un producto con id " + productoId + "."));
        }
        if (codigo == null || codigo.isBlank()) {
            throw new ReglaNegocioException("Indicá el producto (id o código).");
        }
        return porCodigo(codigo);
    }

    public Producto porCodigo(String codigo) {
        String c = codigo.trim();
        return productos.findBySkuIgnoreCase(c)
                .or(() -> productos.findByCodigoAlternativo(c))
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un producto con código " + c + "."));
    }

    public Ubicacion ubicacion(String codigo) {
        String c = Ubicacion.normalizar(codigo);
        return ubicaciones.findByCodigoIgnoreCase(c)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la ubicación " + c + "."));
    }

    /** La ubicación indicada, o la principal del producto si no se indicó ninguna. */
    public Ubicacion ubicacionODefecto(String codigo, Producto p) {
        if (codigo != null && !codigo.isBlank()) return ubicacion(codigo);
        return asegurarUbicacion(p.getUbicacion());
    }

    /** Crea la ubicación si no existe (la principal de un producto es texto libre en el alta). */
    @Transactional
    public Ubicacion asegurarUbicacion(String codigo) {
        String c = Ubicacion.normalizar(codigo);
        return ubicaciones.findByCodigoIgnoreCase(c)
                .orElseGet(() -> ubicaciones.save(new Ubicacion(c, "", "")));
    }

    // ------------------------------------------------------------------
    // Idempotencia
    // ------------------------------------------------------------------

    public boolean yaProcesada(String clientOpId) {
        return clientOpId != null && !clientOpId.isBlank() && operaciones.existsById(clientOpId.trim());
    }

    @Transactional
    public void marcarProcesada(String clientOpId, Sesion sesion) {
        if (clientOpId != null && !clientOpId.isBlank()) {
            operaciones.save(new OperacionProcesada(clientOpId.trim(), sesion.codigo()));
        }
    }

    // ------------------------------------------------------------------

    private String loteValido(Producto p, String lote) {
        String l = lote == null ? "" : lote.trim();
        if (p.isControlaLote() && l.isEmpty()) {
            throw new ReglaNegocioException(p.getSku() + " se controla por lote: indicá el lote.");
        }
        return l;
    }

    private static void exigirPositiva(Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new ReglaNegocioException("La cantidad tiene que ser mayor a 0.");
        }
    }
}
