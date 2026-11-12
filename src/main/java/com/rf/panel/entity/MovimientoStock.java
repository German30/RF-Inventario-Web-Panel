package com.rf.panel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una fila del historial: quién tocó qué producto, cuándo y qué cambió.
 *
 * Denormaliza `sku`, `operatorCode` y `operatorName` a propósito: la fila
 * tiene que seguir teniendo sentido aunque el producto se borre después o el
 * operario cambie de nombre — es una bitácora, no una vista.
 */
@Entity
@Table(name = "stock_movements")
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productoId;

    @Column(name = "sku", nullable = false, length = 128)
    private String sku;

    @Column(name = "operator_code", nullable = false, length = 32)
    private String operadorCodigo;

    @Column(name = "operator_name", nullable = false, length = 255)
    private String operadorNombre = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private AccionMovimiento accion;

    @Column(name = "delta", nullable = false)
    private int delta;

    @Column(name = "quantity_after", nullable = false)
    private int cantidadDespues;

    @Column(name = "created_at", nullable = false)
    private long creadoEn;

    @Column(name = "reason", nullable = false, length = 255)
    private String motivo = "";

    /** Nº de pedido, remito, conteo... lo que justifique el movimiento. */
    @Column(name = "reference", nullable = false, length = 100)
    private String referencia = "";

    @Column(name = "location_code", nullable = false, length = 128)
    private String ubicacion = "";

    @Column(name = "to_location_code", nullable = false, length = 128)
    private String ubicacionDestino = "";

    @Column(name = "lot", nullable = false, length = 64)
    private String lote = "";

    protected MovimientoStock() {
        // JPA
    }

    public MovimientoStock(Long productoId, String sku, String operadorCodigo, String operadorNombre,
                           AccionMovimiento accion, int delta, int cantidadDespues) {
        this.productoId = productoId;
        this.sku = sku;
        this.operadorCodigo = operadorCodigo;
        this.operadorNombre = operadorNombre == null ? "" : operadorNombre;
        this.accion = accion;
        this.delta = delta;
        this.cantidadDespues = cantidadDespues;
        this.creadoEn = System.currentTimeMillis();
    }

    /** Detalle opcional; los textos se recortan al largo de la columna. */
    public MovimientoStock conDetalle(String motivo, String referencia, String ubicacion,
                                      String ubicacionDestino, String lote) {
        this.motivo = recortar(motivo, 255);
        this.referencia = recortar(referencia, 100);
        this.ubicacion = recortar(ubicacion, 128);
        this.ubicacionDestino = recortar(ubicacionDestino, 128);
        this.lote = recortar(lote, 64);
        return this;
    }

    private static String recortar(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public String getSku() {
        return sku;
    }

    public String getOperadorCodigo() {
        return operadorCodigo;
    }

    public String getOperadorNombre() {
        return operadorNombre;
    }

    public AccionMovimiento getAccion() {
        return accion;
    }

    public int getDelta() {
        return delta;
    }

    public int getCantidadDespues() {
        return cantidadDespues;
    }

    public long getCreadoEn() {
        return creadoEn;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getReferencia() {
        return referencia;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public String getUbicacionDestino() {
        return ubicacionDestino;
    }

    public String getLote() {
        return lote;
    }
}
