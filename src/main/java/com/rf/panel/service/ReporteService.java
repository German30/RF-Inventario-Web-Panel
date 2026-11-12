package com.rf.panel.service;

import com.rf.panel.entity.AccionMovimiento;
import com.rf.panel.entity.Existencia;
import com.rf.panel.entity.MovimientoStock;
import com.rf.panel.entity.Producto;
import com.rf.panel.repository.ExistenciaRepository;
import com.rf.panel.repository.MovimientoStockRepository;
import com.rf.panel.repository.ProductoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Exportaciones CSV (separador `;` y BOM UTF-8: Excel en español las abre
 * directo con acentos y columnas bien separadas).
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final String BOM = "\uFEFF";
    private static final int MAX_MOVIMIENTOS = 50_000;

    private final ProductoRepository productos;
    private final ExistenciaRepository existencias;
    private final MovimientoStockRepository movimientos;

    public ReporteService(ProductoRepository productos, ExistenciaRepository existencias,
                          MovimientoStockRepository movimientos) {
        this.productos = productos;
        this.existencias = existencias;
        this.movimientos = movimientos;
    }

    public String inventario() {
        StringBuilder sb = new StringBuilder(BOM)
                .append("SKU;Nombre;Categoría;Unidad;Cantidad;Stock mínimo;Bajo mínimo;Costo unitario;Valor;Ubicación principal;Códigos alternativos\n");
        for (Producto p : productos.findAll(Sort.by("nombre"))) {
            BigDecimal valor = p.getCostoUnitario().multiply(BigDecimal.valueOf(p.getCantidad()));
            fila(sb, p.getSku(), p.getNombre(), p.getCategoria(), p.getUnidad(), p.getCantidad(), p.getStockMinimo(),
                    p.bajoMinimo() ? "SI" : "NO", p.getCostoUnitario(), valor, p.getUbicacion(),
                    String.join(" ", p.getCodigosAlternativos()));
        }
        return sb.toString();
    }

    public String existencias() {
        StringBuilder sb = new StringBuilder(BOM).append("SKU;Nombre;Ubicación;Lote;Vence;Cantidad\n");
        for (Producto p : productos.findAll(Sort.by("nombre"))) {
            for (Existencia e : existencias.conStock(p.getId())) {
                fila(sb, p.getSku(), p.getNombre(), e.getUbicacion().getCodigo(), e.getLote(),
                        e.getVence() == null ? "" : e.getVence(), e.getCantidad());
            }
        }
        return sb.toString();
    }

    public String movimientos(Long desde, Long hasta, String sku, String operador, AccionMovimiento accion) {
        StringBuilder sb = new StringBuilder(BOM)
                .append("Fecha;SKU;Acción;Delta;Total después;Ubicación;Destino;Lote;Motivo;Referencia;Operario;Nombre\n");
        List<MovimientoStock> ms = movimientos.buscar(desde, hasta, blankANull(sku), blankANull(operador), accion,
                PageRequest.of(0, MAX_MOVIMIENTOS)).getContent();
        for (MovimientoStock m : ms) {
            fila(sb, Instant.ofEpochMilli(m.getCreadoEn()), m.getSku(), m.getAccion(), m.getDelta(),
                    m.getCantidadDespues(), m.getUbicacion(), m.getUbicacionDestino(), m.getLote(), m.getMotivo(),
                    m.getReferencia(), m.getOperadorCodigo(), m.getOperadorNombre());
        }
        return sb.toString();
    }

    private static void fila(StringBuilder sb, Object... valores) {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) sb.append(';');
            sb.append(celda(valores[i]));
        }
        sb.append('\n');
    }

    /** Escapa comillas/separadores y neutraliza fórmulas (=,+,-,@) para evitar CSV injection. */
    static String celda(Object valor) {
        if (valor == null) return "";
        String s = valor.toString();
        if (!(valor instanceof Number) && !s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "'" + s;
        }
        if (s.contains(";") || s.contains("\"") || s.contains("\n")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String blankANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
