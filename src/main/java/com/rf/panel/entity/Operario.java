package com.rf.panel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Fila de la tabla `users`: el operario entra a la app Android con su código y
 * PIN; si es {@link RolOperario#SUPERVISOR} también puede entrar al panel web
 * y gestionar operarios (ver `OperarioAdminController`, gateado por rol en el
 * servidor, no solo escondido en la UI).
 *
 * `pin_hash` es BCrypt (ver {@code V2__pin_hash_bcrypt.sql}), no el PIN.
 */
@Entity
@Table(name = "users")
public class Operario {

    @Id
    @Column(name = "code", length = 32)
    private String codigo;

    @Column(name = "pin_hash", nullable = false, length = 72)
    private String pinHash;

    @Column(name = "name", nullable = false, length = 255)
    private String nombre = "";

    @Column(name = "active", nullable = false)
    private boolean activo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private RolOperario rol = RolOperario.OPERARIO;

    protected Operario() {
        // JPA
    }

    public Operario(String codigo, String pinHash, String nombre) {
        this(codigo, pinHash, nombre, RolOperario.OPERARIO);
    }

    public Operario(String codigo, String pinHash, String nombre, RolOperario rol) {
        this.codigo = codigo;
        this.pinHash = pinHash;
        this.nombre = nombre == null ? "" : nombre;
        this.rol = rol == null ? RolOperario.OPERARIO : rol;
    }

    public void actualizar(String nombre, RolOperario rol, boolean activo) {
        this.nombre = nombre == null ? "" : nombre;
        this.rol = rol == null ? this.rol : rol;
        this.activo = activo;
    }

    public void cambiarPin(String nuevoPinHash) {
        this.pinHash = nuevoPinHash;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getPinHash() {
        return pinHash;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public RolOperario getRol() {
        return rol;
    }
}
