package com.rf.panel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Un producto/ubicación/lote dentro de un {@link Conteo}: esperado (foto al abrir) vs. contado. */
@Entity
@Table(name = "count_lines")
public class ConteoLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Conteo conteo;

    @Column(name = "product_id", nullable = false)
    private Long productoId;

    @Column(name = "sku", nullable = false, length = 128)
    private String sku;

    @Column(name = "name", nullable = false, length = 255)
    private String nombre = "";

    @Column(name = "location_code", nullable = false, length = 128)
    private String ubicacion;

    @Column(name = "lot", nullable = false, length = 64)
    private String lote = "";

    @Column(name = "expected_qty", nullable = false)
    private int esperado;

    /** null = todavía no contado. */
    @Column(name = "counted_qty")
    private Integer contado;

    @Column(name = "counted_by", length = 32)
    private String contadoPor;

    @Column(name = "counted_at")
    private Long contadoEn;

    protected ConteoLinea() {
        // JPA
    }

    public ConteoLinea(Long productoId, String sku, String nombre, String ubicacion, String lote, int esperado) {
        this.productoId = productoId;
        this.sku = sku;
        this.nombre = nombre == null ? "" : nombre;
        this.ubicacion = ubicacion;
        this.lote = lote == null ? "" : lote;
        this.esperado = esperado;
    }

    void asignar(Conteo conteo) {
        this.conteo = conteo;
    }

    public void registrar(int cantidad, boolean sumar, String operador) {
        int base = (sumar && contado != null) ? contado : 0;
        this.contado = Math.max(0, base + cantidad);
        this.contadoPor = operador;
        this.contadoEn = System.currentTimeMillis();
    }

    public boolean coincide(Long productoId, String ubicacion, String lote) {
        return this.productoId.equals(productoId)
                && this.ubicacion.equalsIgnoreCase(ubicacion)
                && this.lote.equalsIgnoreCase(lote == null ? "" : lote);
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

    public String getNombre() {
        return nombre;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public String getLote() {
        return lote;
    }

    public int getEsperado() {
        return esperado;
    }

    public Integer getContado() {
        return contado;
    }

    public String getContadoPor() {
        return contadoPor;
    }

    public Long getContadoEn() {
        return contadoEn;
    }
}
