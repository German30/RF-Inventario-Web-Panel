package com.rf.panel.service;

import com.rf.panel.dto.ConteoRequest;
import com.rf.panel.dto.ConteoResponse;
import com.rf.panel.entity.AccionMovimiento;
import com.rf.panel.entity.Conteo;
import com.rf.panel.entity.ConteoLinea;
import com.rf.panel.entity.Existencia;
import com.rf.panel.entity.Producto;
import com.rf.panel.entity.Ubicacion;
import com.rf.panel.exception.RecursoNoEncontradoException;
import com.rf.panel.exception.ReglaNegocioException;
import com.rf.panel.repository.ConteoRepository;
import com.rf.panel.repository.ExistenciaRepository;
import com.rf.panel.repository.ProductoRepository;
import com.rf.panel.security.Sesion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Conteo cíclico: abrir → escanear/contar → cerrar → (supervisor) aplicar.
 *
 * Al abrir un conteo de ubicación se toma la "foto" de lo esperado. Al
 * aplicar, cada línea lleva la existencia real a lo contado (no a la foto):
 * así los movimientos que ocurrieron durante el conteo no se pisan dos veces.
 */
@Service
@Transactional(readOnly = true)
public class ConteoService {

    private final ConteoRepository repository;
    private final ExistenciaRepository existencias;
    private final ProductoRepository productos;
    private final InventarioService inventario;

    public ConteoService(ConteoRepository repository, ExistenciaRepository existencias,
                         ProductoRepository productos, InventarioService inventario) {
        this.repository = repository;
        this.existencias = existencias;
        this.productos = productos;
        this.inventario = inventario;
    }

    public List<ConteoResponse> listar(boolean soloAbiertos, Sesion sesion) {
        List<Conteo> conteos = soloAbiertos
                ? repository.findByEstadoInOrderByCreadoEnDesc(EnumSet.of(Conteo.Estado.ABIERTO, Conteo.Estado.CERRADO))
                : repository.findTop100ByOrderByCreadoEnDesc();
        return conteos.stream().map(c -> ConteoResponse.from(c, mostrarEsperado(c, sesion), false)).toList();
    }

    public ConteoResponse obtener(long id, Sesion sesion) {
        Conteo c = buscar(id);
        return ConteoResponse.from(c, mostrarEsperado(c, sesion), true);
    }

    @Transactional
    public ConteoResponse abrir(ConteoRequest.Abrir req, Sesion sesion) {
        boolean ciego = req.ciego() == null || req.ciego();
        String codigoUbicacion = req.ubicacion() == null || req.ubicacion().isBlank()
                ? "" : inventario.ubicacion(req.ubicacion()).getCodigo();
        Conteo c = new Conteo(codigoUbicacion, ciego, req.nota(), sesion.codigo(), sesion.nombre());

        if (!codigoUbicacion.isEmpty()) {
            // Foto de lo esperado: todo lo que el sistema cree que hay en la ubicación.
            Ubicacion u = inventario.ubicacion(codigoUbicacion);
            List<Existencia> es = existencias.enUbicacion(u.getId());
            Map<Long, Producto> porId = productos.findAllById(es.stream().map(Existencia::getProductoId).toList())
                    .stream().collect(Collectors.toMap(Producto::getId, Function.identity()));
            for (Existencia e : es) {
                Producto p = porId.get(e.getProductoId());
                c.agregarLinea(new ConteoLinea(p.getId(), p.getSku(), p.getNombre(), u.getCodigo(),
                        e.getLote(), e.getCantidad()));
            }
        }
        return ConteoResponse.from(repository.save(c), mostrarEsperado(c, sesion), true);
    }

    @Transactional
    public ConteoResponse registrar(long id, ConteoRequest.Linea req, Sesion sesion) {
        Conteo c = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un conteo con id " + id + "."));
        if (inventario.yaProcesada(req.clientOpId())) {
            return ConteoResponse.from(c, mostrarEsperado(c, sesion), true);
        }
        if (c.getEstado() != Conteo.Estado.ABIERTO) {
            throw new ReglaNegocioException("El conteo #" + id + " no está abierto.");
        }
        Producto p = inventario.porCodigo(req.codigo());
        Ubicacion u;
        if (c.esDeUbicacion()) {
            u = inventario.ubicacion(c.getUbicacion());
            if (req.ubicacion() != null && !req.ubicacion().isBlank()
                    && !Ubicacion.normalizar(req.ubicacion()).equals(u.getCodigo())) {
                throw new ReglaNegocioException("Este conteo es de " + u.getCodigo() + ", no de "
                        + Ubicacion.normalizar(req.ubicacion()) + ".");
            }
        } else {
            u = inventario.ubicacionODefecto(req.ubicacion(), p);
        }
        String lote = req.lote() == null ? "" : req.lote().trim();
        boolean sumar = req.sumar() == null || req.sumar();

        ConteoLinea linea = c.getLineas().stream()
                .filter(l -> l.coincide(p.getId(), u.getCodigo(), lote))
                .findFirst()
                .orElseGet(() -> c.agregarLinea(new ConteoLinea(p.getId(), p.getSku(), p.getNombre(),
                        u.getCodigo(), lote, inventario.cantidadEn(p, u, lote))));
        linea.registrar(req.cantidad(), sumar, sesion.codigo());
        inventario.marcarProcesada(req.clientOpId(), sesion);
        return ConteoResponse.from(c, mostrarEsperado(c, sesion), true);
    }

    @Transactional
    public ConteoResponse cerrar(long id, Sesion sesion) {
        Conteo c = buscar(id);
        if (c.getEstado() != Conteo.Estado.ABIERTO) {
            throw new ReglaNegocioException("El conteo #" + id + " no está abierto.");
        }
        c.cerrar();
        return ConteoResponse.from(c, mostrarEsperado(c, sesion), true);
    }

    @Transactional
    public ConteoResponse reabrir(long id, Sesion sesion) {
        Conteo c = buscar(id);
        if (c.getEstado() != Conteo.Estado.CERRADO) {
            throw new ReglaNegocioException("Solo se puede reabrir un conteo cerrado.");
        }
        c.reabrir();
        return ConteoResponse.from(c, true, true);
    }

    /** Lleva el stock real a lo contado y deja un movimiento CONTEO por cada diferencia. */
    @Transactional
    public ConteoResponse aplicar(long id, Sesion sesion) {
        Conteo c = buscar(id);
        if (c.getEstado() != Conteo.Estado.ABIERTO && c.getEstado() != Conteo.Estado.CERRADO) {
            throw new ReglaNegocioException("El conteo #" + id + " ya fue " + c.getEstado().name().toLowerCase() + ".");
        }
        String referencia = "CONTEO-" + c.getId();
        for (ConteoLinea l : c.getLineas()) {
            Integer contado = l.getContado();
            if (contado == null) {
                if (!c.esDeUbicacion()) continue; // conteo libre: lo no contado no se toca
                contado = 0;                      // conteo de ubicación: no estaba
            }
            Producto p = inventario.bloquear(l.getProductoId());
            Ubicacion u = inventario.ubicacion(l.getUbicacion());
            int delta = inventario.fijarEn(p, u, l.getLote(), contado);
            if (delta != 0) {
                inventario.registrar(p, sesion, AccionMovimiento.CONTEO, delta, u.getCodigo(), "", l.getLote(),
                        "Diferencia de conteo", referencia);
            }
        }
        c.aplicar(sesion.codigo());
        return ConteoResponse.from(c, true, true);
    }

    @Transactional
    public ConteoResponse cancelar(long id, Sesion sesion) {
        Conteo c = buscar(id);
        if (c.getEstado() == Conteo.Estado.APLICADO) {
            throw new ReglaNegocioException("Un conteo aplicado no se puede cancelar.");
        }
        c.cancelar();
        return ConteoResponse.from(c, true, true);
    }

    private boolean mostrarEsperado(Conteo c, Sesion sesion) {
        return !c.isCiego() || sesion.esSupervisor() || c.getEstado() != Conteo.Estado.ABIERTO;
    }

    private Conteo buscar(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un conteo con id " + id + "."));
    }
}
