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

import java.time.LocalDate;

/**
 * Cuánto hay de un producto en una ubicación y lote (`stock_levels`). Lote
 * vacío = sin lote. Solo la modifica {@code InventarioService}, siempre con el
 * producto bloqueado (SELECT ... FOR UPDATE), así que no necesita lock propio.
 */
@Entity
@Table(name = "stock_levels")
public class Existencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productoId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Ubicacion ubicacion;

    @Column(name = "lot", nullable = false, length = 64)
    private String lote = "";

    @Column(name = "expires_on")
    private LocalDate vence;

    @Column(name = "quantity", nullable = false)
    private int cantidad;

    protected Existencia() {
        // JPA
    }

    public Existencia(Long productoId, Ubicacion ubicacion, String lote, LocalDate vence) {
        this.productoId = productoId;
        this.ubicacion = ubicacion;
        this.lote = lote == null ? "" : lote.trim();
        this.vence = vence;
    }

    public void sumar(int delta) {
        this.cantidad += delta;
    }

    public void fijar(int cantidad) {
        this.cantidad = cantidad;
    }

    public void fijarVencimiento(LocalDate vence) {
        if (vence != null) this.vence = vence;
    }

    public Long getId() {
        return id;
    }

    public Long getProductoId() {
        return productoId;
    }

    public Ubicacion getUbicacion() {
        return ubicacion;
    }

    public String getLote() {
        return lote;
    }

    public LocalDate getVence() {
        return vence;
    }

    public int getCantidad() {
        return cantidad;
    }
}
