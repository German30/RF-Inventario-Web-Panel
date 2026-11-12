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
 * Sesión de conteo cíclico. Con `ubicacion` es un conteo de esa ubicación
 * (lo que no se cuente se considera 0 al aplicar); sin ella es un conteo
 * libre, que solo ajusta lo efectivamente contado.
 */
@Entity
@Table(name = "count_sessions")
public class Conteo {

    public enum Estado { ABIERTO, CERRADO, APLICADO, CANCELADO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "location_code", nullable = false, length = 128)
    private String ubicacion = "";

    /** Conteo ciego: el operario no ve la cantidad esperada (evita "contar para que dé"). */
    @Column(name = "blind", nullable = false)
    private boolean ciego = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Estado estado = Estado.ABIERTO;

    @Column(name = "note", nullable = false, length = 500)
    private String nota = "";

    @Column(name = "created_by_code", nullable = false, length = 32)
    private String creadoPorCodigo;

    @Column(name = "created_by_name", nullable = false, length = 255)
    private String creadoPorNombre = "";

    @Column(name = "created_at", nullable = false)
    private long creadoEn;

    @Column(name = "closed_at")
    private Long cerradoEn;

    @Column(name = "applied_at")
    private Long aplicadoEn;

    @Column(name = "applied_by", length = 32)
    private String aplicadoPor;

    @OneToMany(mappedBy = "conteo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sku ASC, ubicacion ASC, lote ASC")
    private List<ConteoLinea> lineas = new ArrayList<>();

    protected Conteo() {
        // JPA
    }

    public Conteo(String ubicacion, boolean ciego, String nota, String creadoPorCodigo, String creadoPorNombre) {
        this.ubicacion = ubicacion == null ? "" : ubicacion;
        this.ciego = ciego;
        this.nota = nota == null ? "" : nota.trim();
        this.creadoPorCodigo = creadoPorCodigo;
        this.creadoPorNombre = creadoPorNombre == null ? "" : creadoPorNombre;
        this.creadoEn = System.currentTimeMillis();
    }

    public ConteoLinea agregarLinea(ConteoLinea linea) {
        linea.asignar(this);
        lineas.add(linea);
        return linea;
    }

    public void cerrar() {
        this.estado = Estado.CERRADO;
        this.cerradoEn = System.currentTimeMillis();
    }

    public void reabrir() {
        this.estado = Estado.ABIERTO;
        this.cerradoEn = null;
    }

    public void aplicar(String codigo) {
        this.estado = Estado.APLICADO;
        this.aplicadoEn = System.currentTimeMillis();
        this.aplicadoPor = codigo;
        if (cerradoEn == null) cerradoEn = aplicadoEn;
    }

    public void cancelar() {
        this.estado = Estado.CANCELADO;
        this.cerradoEn = System.currentTimeMillis();
    }

    public boolean esDeUbicacion() {
        return !ubicacion.isBlank();
    }

    public Long getId() {
        return id;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public boolean isCiego() {
        return ciego;
    }

    public Estado getEstado() {
        return estado;
    }

    public String getNota() {
        return nota;
    }

    public String getCreadoPorCodigo() {
        return creadoPorCodigo;
    }

    public String getCreadoPorNombre() {
        return creadoPorNombre;
    }

    public long getCreadoEn() {
        return creadoEn;
    }

    public Long getCerradoEn() {
        return cerradoEn;
    }

    public Long getAplicadoEn() {
        return aplicadoEn;
    }

    public String getAplicadoPor() {
        return aplicadoPor;
    }

    public List<ConteoLinea> getLineas() {
        return lineas;
    }
}
