package com.rf.panel.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Pedido de COMPRA (se recibe: suma stock) o de VENTA (se prepara/pickea:
 * descuenta stock). Las líneas avanzan con cada escaneo del operario.
 */
@Entity
@Table(name = "orders")
public class Pedido {

    public enum Tipo { COMPRA, VENTA }

    public enum Estado { PENDIENTE, EN_PROCESO, COMPLETADO, CANCELADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private Tipo tipo;

    @Column(name = "number", nullable = false, length = 64, unique = true)
    private String numero;

    /** Proveedor (compra) o cliente (venta). */
    @Column(name = "party", nullable = false, length = 255)
    private String contraparte = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    @Column(name = "note", nullable = false, length = 500)
    private String nota = "";

    @Column(name = "created_by", nullable = false, length = 32)
    private String creadoPor;

    @Column(name = "created_at", nullable = false)
    private long creadoEn;

    @Column(name = "completed_at")
    private Long completadoEn;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PedidoLinea> lineas = new ArrayList<>();

    protected Pedido() {
        // JPA
    }

    public Pedido(Tipo tipo, String numero, String contraparte, String nota, String creadoPor) {
        this.tipo = tipo;
        this.numero = numero.trim();
        this.contraparte = contraparte == null ? "" : contraparte.trim();
        this.nota = nota == null ? "" : nota.trim();
        this.creadoPor = creadoPor;
        this.creadoEn = System.currentTimeMillis();
    }

    public void agregarLinea(PedidoLinea linea) {
        linea.asignar(this);
        lineas.add(linea);
    }

    public void marcarEnProceso() {
        if (estado == Estado.PENDIENTE) estado = Estado.EN_PROCESO;
    }

    public void completar() {
        estado = Estado.COMPLETADO;
        completadoEn = System.currentTimeMillis();
    }

    public void cancelar() {
        estado = Estado.CANCELADO;
        completadoEn = System.currentTimeMillis();
    }

    public boolean abierto() {
        return estado == Estado.PENDIENTE || estado == Estado.EN_PROCESO;
    }

    public boolean todoHecho() {
        return lineas.stream().allMatch(l -> l.getHecho() >= l.getPedido());
    }

    public Long getId() {
        return id;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getNumero() {
        return numero;
    }

    public String getContraparte() {
        return contraparte;
    }

    public Estado getEstado() {
        return estado;
    }

    public String getNota() {
        return nota;
    }

    public String getCreadoPor() {
        return creadoPor;
    }

    public long getCreadoEn() {
        return creadoEn;
    }

    public Long getCompletadoEn() {
        return completadoEn;
    }

    public List<PedidoLinea> getLineas() {
        return lineas;
    }
}
