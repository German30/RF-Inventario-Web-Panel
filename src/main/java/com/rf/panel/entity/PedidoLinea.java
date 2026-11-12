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

@Entity
@Table(name = "order_lines")
public class PedidoLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Pedido pedido;

    @Column(name = "product_id", nullable = false)
    private Long productoId;

    @Column(name = "sku", nullable = false, length = 128)
    private String sku;

    @Column(name = "name", nullable = false, length = 255)
    private String nombre = "";

    @Column(name = "qty_ordered", nullable = false)
    private int cantidadPedida;

    @Column(name = "qty_done", nullable = false)
    private int cantidadHecha;

    protected PedidoLinea() {
        // JPA
    }

    public PedidoLinea(Long productoId, String sku, String nombre, int cantidadPedida) {
        this.productoId = productoId;
        this.sku = sku;
        this.nombre = nombre == null ? "" : nombre;
        this.cantidadPedida = cantidadPedida;
    }

    void asignar(Pedido pedido) {
        this.pedido = pedido;
    }

    public int pendiente() {
        return Math.max(0, cantidadPedida - cantidadHecha);
    }

    public void avanzar(int cantidad) {
        this.cantidadHecha += cantidad;
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

    public int getPedido() {
        return cantidadPedida;
    }

    public int getHecho() {
        return cantidadHecha;
    }
}
