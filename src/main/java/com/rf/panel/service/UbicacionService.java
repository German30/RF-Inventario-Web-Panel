package com.rf.panel.service;

import com.rf.panel.dto.ExistenciaResponse;
import com.rf.panel.dto.UbicacionRequest;
import com.rf.panel.dto.UbicacionResponse;
import com.rf.panel.entity.Existencia;
import com.rf.panel.entity.Producto;
import com.rf.panel.entity.Ubicacion;
import com.rf.panel.exception.RecursoNoEncontradoException;
import com.rf.panel.exception.ReglaNegocioException;
import com.rf.panel.repository.ExistenciaRepository;
import com.rf.panel.repository.ProductoRepository;
import com.rf.panel.repository.UbicacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UbicacionService {

    private final UbicacionRepository repository;
    private final ExistenciaRepository existencias;
    private final ProductoRepository productos;

    public UbicacionService(UbicacionRepository repository, ExistenciaRepository existencias,
                            ProductoRepository productos) {
        this.repository = repository;
        this.existencias = existencias;
        this.productos = productos;
    }

    public List<UbicacionResponse> listar() {
        return repository.findAllByOrderByCodigoAsc().stream().map(UbicacionResponse::from).toList();
    }

    public UbicacionResponse obtener(String codigo) {
        return UbicacionResponse.from(buscar(codigo));
    }

    /** Qué hay en la ubicación: lo que el operario ve al escanear la etiqueta de un estante. */
    public List<ExistenciaResponse> contenido(String codigo) {
        Ubicacion u = buscar(codigo);
        List<Existencia> es = existencias.enUbicacion(u.getId());
        Map<Long, Producto> porId = productos.findAllById(es.stream().map(Existencia::getProductoId).toList())
                .stream().collect(Collectors.toMap(Producto::getId, Function.identity()));
        return es.stream().map(e -> ExistenciaResponse.from(e, porId.get(e.getProductoId()))).toList();
    }

    @Transactional
    public UbicacionResponse crear(UbicacionRequest req) {
        String codigo = Ubicacion.normalizar(req.codigo());
        if (repository.findByCodigoIgnoreCase(codigo).isPresent()) {
            throw new ReglaNegocioException("Ya existe la ubicación " + codigo + ".");
        }
        Ubicacion u = new Ubicacion(codigo, req.descripcion(), req.zona());
        if (Boolean.FALSE.equals(req.activa())) u.actualizar(req.descripcion(), req.zona(), false);
        return UbicacionResponse.from(repository.save(u));
    }

    @Transactional
    public UbicacionResponse actualizar(String codigo, UbicacionRequest req) {
        Ubicacion u = buscar(codigo);
        boolean activa = req.activa() == null || req.activa();
        if (!activa && existencias.existsByUbicacionIdAndCantidadGreaterThan(u.getId(), 0)) {
            throw new ReglaNegocioException("No se puede desactivar " + u.getCodigo() + ": todavía tiene stock.");
        }
        u.actualizar(req.descripcion(), req.zona(), activa);
        return UbicacionResponse.from(u);
    }

    /** Las ubicaciones nunca se borran (el historial las nombra): se desactivan. */
    @Transactional
    public void desactivar(String codigo) {
        Ubicacion u = buscar(codigo);
        actualizar(u.getCodigo(), new UbicacionRequest(u.getCodigo(), u.getDescripcion(), u.getZona(), false));
    }

    private Ubicacion buscar(String codigo) {
        String c = Ubicacion.normalizar(codigo);
        return repository.findByCodigoIgnoreCase(c)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la ubicación " + c + "."));
    }
}
