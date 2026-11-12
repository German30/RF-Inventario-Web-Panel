package com.rf.panel.service;

import com.rf.panel.dto.ExistenciaResponse;
import com.rf.panel.dto.MovimientoStockResponse;
import com.rf.panel.dto.PaginaResponse;
import com.rf.panel.dto.ProductoRequest;
import com.rf.panel.dto.ProductoResponse;
import com.rf.panel.entity.AccionMovimiento;
import com.rf.panel.entity.Producto;
import com.rf.panel.entity.Ubicacion;
import com.rf.panel.exception.ConflictoEdicionException;
import com.rf.panel.exception.ReglaNegocioException;
import com.rf.panel.repository.ExistenciaRepository;
import com.rf.panel.repository.MovimientoStockRepository;
import com.rf.panel.repository.ProductoRepository;
import com.rf.panel.security.Sesion;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ficha de producto (alta/edición/baja/consulta). Todo lo que cambia stock
 * delega en {@link InventarioService}.
 */
@Service
@Transactional(readOnly = true)
public class ProductoService {

    private static final Sort POR_NOMBRE = Sort.by(Sort.Order.asc("nombre").ignoreCase());
    private static final int TAMANIO_MAXIMO = 200;

    private final ProductoRepository repository;
    private final MovimientoStockRepository movimientos;
    private final ExistenciaRepository existencias;
    private final InventarioService inventario;

    public ProductoService(ProductoRepository repository, MovimientoStockRepository movimientos,
                           ExistenciaRepository existencias, InventarioService inventario) {
        this.repository = repository;
        this.movimientos = movimientos;
        this.existencias = existencias;
        this.inventario = inventario;
    }

    public List<ProductoResponse> listar(String busqueda) {
        List<Producto> productos = (busqueda == null || busqueda.isBlank())
                ? repository.findAll(POR_NOMBRE)
                : repository.findByNombreContainingIgnoreCaseOrSkuContainingIgnoreCase(
                        busqueda.trim(), busqueda.trim(), POR_NOMBRE);
        return productos.stream().map(ProductoResponse::from).toList();
    }

    public PaginaResponse<ProductoResponse> pagina(String q, String categoria, String filtro, int pagina, int tamanio) {
        var pageable = PageRequest.of(Math.max(0, pagina), Math.min(Math.max(1, tamanio), TAMANIO_MAXIMO), POR_NOMBRE);
        return PaginaResponse.de(repository.buscar(blankANull(q), blankANull(categoria), blankANull(filtro), pageable),
                ProductoResponse::from);
    }

    public List<String> categorias() {
        return repository.categorias();
    }

    public ProductoResponse obtener(long id) {
        return ProductoResponse.from(inventario.resolver(id, null));
    }

    /** Búsqueda exacta por lo escaneado: SKU o cualquier código alternativo. */
    public ProductoResponse obtenerPorSku(String codigo) {
        return ProductoResponse.from(inventario.porCodigo(codigo));
    }

    public List<ExistenciaResponse> existencias(long id) {
        Producto p = inventario.resolver(id, null);
        return existencias.conStock(p.getId()).stream().map(e -> ExistenciaResponse.from(e, p)).toList();
    }

    /** Últimos movimientos del producto: quién lo tocó, cuándo y qué cambió. */
    public List<MovimientoStockResponse> listarMovimientos(long id) {
        return movimientos.findTop50ByProductoIdOrderByCreadoEnDesc(id).stream()
                .map(MovimientoStockResponse::from)
                .toList();
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest req, Sesion sesion) {
        String sku = req.sku().trim();
        if (repository.existsBySkuIgnoreCase(sku) || repository.codigoEnUso(sku, null)) {
            throw new ReglaNegocioException("Ya existe un producto con el SKU " + sku + ".");
        }
        Ubicacion principal = inventario.asegurarUbicacion(req.ubicacion());
        Producto p = repository.save(new Producto(req.nombre().trim(), sku, 0, principal.getCodigo()));
        aplicarFicha(p, req);

        if (req.cantidad() > 0) {
            if (p.isControlaLote()) {
                throw new ReglaNegocioException(sku + " se controla por lote: cargá el stock inicial con una "
                        + "entrada indicando el lote.");
            }
            inventario.ingresar(p, principal, "", null, req.cantidad());
        }
        inventario.registrar(p, sesion, AccionMovimiento.CREACION, p.getCantidad(), principal.getCodigo(), "", "",
                "", "");
        return ProductoResponse.from(p);
    }

    @Transactional
    public ProductoResponse actualizar(long id, ProductoRequest req, Sesion sesion) {
        Producto producto = inventario.bloquear(id);

        // Optimistic locking: si el cliente venía de una lectura vieja, que se
        // entere en vez de pisar en silencio lo que guardó otro operario.
        if (req.actualizadoEn() != null && !req.actualizadoEn().equals(producto.getActualizadoEn())) {
            throw new ConflictoEdicionException(
                    "Otro cambio se guardó mientras editabas " + producto.getSku() + ".", producto);
        }

        String sku = req.sku().trim();
        if (repository.codigoEnUso(sku, producto.getId())) {
            throw new ReglaNegocioException("Otro producto ya usa el código " + sku + ".");
        }

        Ubicacion principal = inventario.asegurarUbicacion(req.ubicacion());
        producto.actualizar(req.nombre().trim(), sku, principal.getCodigo());
        aplicarFicha(producto, req);

        // La cantidad del formulario es el total deseado: la diferencia se
        // asienta como ajuste (entra en la principal, sale FEFO).
        int diferencia = req.cantidad() - producto.getCantidad();
        if (diferencia != 0) {
            inventario.ajustar(producto, sesion, diferencia, diferencia > 0 ? principal.getCodigo() : null,
                    null, null, "Edición de ficha", "");
        }
        inventario.registrar(producto, sesion, AccionMovimiento.EDICION, 0, principal.getCodigo(), "", "", "", "");
        return ProductoResponse.from(producto);
    }

    @Transactional
    public ProductoResponse ajustarStock(long id, int delta, Sesion sesion) {
        if (delta == 0) {
            throw new ReglaNegocioException("El ajuste no puede ser 0.");
        }
        Producto producto = inventario.bloquear(id);
        inventario.ajustar(producto, sesion, delta, null, null, null, "", "");
        return ProductoResponse.from(producto);
    }

    @Transactional
    public void eliminar(long id, Sesion sesion) {
        Producto producto = inventario.bloquear(id);
        int antes = producto.getCantidad();
        producto.fijarCantidadTotal(0);
        inventario.registrar(producto, sesion, AccionMovimiento.ELIMINACION, -antes,
                producto.getUbicacion(), "", "", "", "");
        existencias.deleteByProductoId(producto.getId());
        repository.delete(producto);
    }

    private void aplicarFicha(Producto p, ProductoRequest req) {
        p.actualizarFicha(req.descripcion(), req.categoria(), req.unidad(), req.stockMinimo(),
                req.costoUnitario(), req.controlaLote());
        if (req.codigosAlternativos() != null) {
            for (String codigo : req.codigosAlternativos()) {
                if (codigo != null && !codigo.isBlank() && repository.codigoEnUso(codigo.trim(), p.getId())) {
                    throw new ReglaNegocioException("El código " + codigo.trim() + " ya lo usa otro producto.");
                }
            }
            p.reemplazarCodigosAlternativos(req.codigosAlternativos());
        }
    }

    private static String blankANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
