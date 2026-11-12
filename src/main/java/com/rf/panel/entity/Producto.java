package com.rf.panel.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Fila de la tabla `products`.
 *
 * Desde V6 `cantidad` es el TOTAL del producto (suma de {@link Existencia} en
 * todas las ubicaciones y lotes) y solo lo recalcula {@code InventarioService};
 * `ubicacion` es la ubicación PRINCIPAL, donde caen los movimientos que no
 * indican una. Los nombres de columna siguen en inglés (esquema heredado de la
 * app Android).
 */
@Entity
@Table(name = "products")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String nombre;

    @Column(name = "sku", nullable = false, length = 128)
    private String sku;

    @Column(name = "quantity", nullable = false)
    private int cantidad;

    @Column(name = "location", nullable = false, length = 255)
    private String ubicacion = "";

    /** Epoch millis. Versión para optimistic locking y cursor de la sync incremental. */
    @Column(name = "updated_at", nullable = false)
    private long actualizadoEn;

    @Column(name = "description", nullable = false, length = 1000)
    private String descripcion = "";

    @Column(name = "category", nullable = false, length = 100)
    private String categoria = "";

    @Column(name = "unit", nullable = false, length = 20)
    private String unidad = "UN";

    @Column(name = "min_stock", nullable = false)
    private int stockMinimo;

    @Column(name = "unit_cost", nullable = false, precision = 14, scale = 2)
    private BigDecimal costoUnitario = BigDecimal.ZERO;

    @Column(name = "tracks_lots", nullable = false)
    private boolean controlaLote;

    /** EAN del proveedor, código interno, etc. Además del SKU, que también se escanea. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_barcodes", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "code", length = 128)
    @BatchSize(size = 200)
    private Set<String> codigosAlternativos = new LinkedHashSet<>();

    protected Producto() {
        // JPA
    }

    /**
     * `cantidad` acá es solo el valor inicial de la columna; el stock real lo
     * asienta {@code InventarioService} como existencia en la ubicación principal.
     */
    public Producto(String nombre, String sku, int cantidad, String ubicacion) {
        this.nombre = nombre;
        this.sku = sku;
        this.cantidad = cantidad;
        this.ubicacion = ubicacion == null ? "" : ubicacion;
        this.actualizadoEn = System.currentTimeMillis();
    }

    public void actualizar(String nombre, String sku, String ubicacion) {
        this.nombre = nombre;
        this.sku = sku;
        this.ubicacion = ubicacion == null ? "" : ubicacion;
        marcarModificado();
    }

    /** Campos opcionales de la ficha: null = no tocar (clientes viejos no los mandan). */
    public void actualizarFicha(String descripcion, String categoria, String unidad, Integer stockMinimo,
                                BigDecimal costoUnitario, Boolean controlaLote) {
        if (descripcion != null) this.descripcion = descripcion.trim();
        if (categoria != null) this.categoria = categoria.trim();
        if (unidad != null && !unidad.isBlank()) this.unidad = unidad.trim().toUpperCase();
        if (stockMinimo != null) this.stockMinimo = Math.max(0, stockMinimo);
        if (costoUnitario != null) this.costoUnitario = costoUnitario.max(BigDecimal.ZERO);
        if (controlaLote != null) this.controlaLote = controlaLote;
        marcarModificado();
    }

    public void reemplazarCodigosAlternativos(Collection<String> codigos) {
        codigosAlternativos.clear();
        codigos.stream()
                .map(String::trim)
                .filter(c -> !c.isEmpty() && !c.equalsIgnoreCase(sku))
                .forEach(codigosAlternativos::add);
        marcarModificado();
    }

    /** Solo lo llama {@code InventarioService} después de tocar las existencias. */
    public void fijarCantidadTotal(int total) {
        this.cantidad = total;
        marcarModificado();
    }

    public void marcarModificado() {
        // Nunca hacia atrás ni repetido: es la versión del optimistic locking y
        // el cursor de la sync incremental de la app.
        this.actualizadoEn = Math.max(System.currentTimeMillis(), actualizadoEn + 1);
    }

    public boolean bajoMinimo() {
        return stockMinimo > 0 && cantidad <= stockMinimo;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getSku() {
        return sku;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public long getActualizadoEn() {
        return actualizadoEn;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getUnidad() {
        return unidad;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public boolean isControlaLote() {
        return controlaLote;
    }

    public Set<String> getCodigosAlternativos() {
        return codigosAlternativos;
    }
}
