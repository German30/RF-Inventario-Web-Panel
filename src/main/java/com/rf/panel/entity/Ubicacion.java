package com.rf.panel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Ubicación física (depósito/pasillo/estante). `codigo` es lo que se imprime y se escanea. */
@Entity
@Table(name = "locations")
public class Ubicacion {

    public static final String GENERAL = "GENERAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 128, unique = true)
    private String codigo;

    @Column(name = "description", nullable = false, length = 255)
    private String descripcion = "";

    @Column(name = "zone", nullable = false, length = 64)
    private String zona = "";

    @Column(name = "active", nullable = false)
    private boolean activa = true;

    protected Ubicacion() {
        // JPA
    }

    public Ubicacion(String codigo, String descripcion, String zona) {
        this.codigo = normalizar(codigo);
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.zona = zona == null ? "" : zona.trim();
    }

    /** Los códigos se guardan en mayúsculas y sin espacios en los bordes: así se escanean. */
    public static String normalizar(String codigo) {
        String c = codigo == null ? "" : codigo.trim().toUpperCase();
        return c.isEmpty() ? GENERAL : c;
    }

    public void actualizar(String descripcion, String zona, boolean activa) {
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.zona = zona == null ? "" : zona.trim();
        this.activa = activa;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getZona() {
        return zona;
    }

    public boolean isActiva() {
        return activa;
    }
}
