package com.rf.panel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Fila de la tabla `panel_admins`: la identidad del panel web, separada de los
 * operarios de la app Android (tabla `users` / {@link Operario}).
 *
 * Misma credencial que un operario —código numérico + PIN con BCrypt— pero es
 * otra tabla y otro login ({@code /api/panel/auth/login}): un operario no puede
 * entrar al panel y un administrador no aparece en el inventario como operario.
 */
@Entity
@Table(name = "panel_admins")
public class Administrador {

    @Id
    @Column(name = "code", length = 32)
    private String codigo;

    @Column(name = "pin_hash", nullable = false, length = 72)
    private String pinHash;

    @Column(name = "name", nullable = false, length = 255)
    private String nombre = "";

    @Column(name = "active", nullable = false)
    private boolean activo = true;

    protected Administrador() {
        // JPA
    }

    public Administrador(String codigo, String pinHash, String nombre) {
        this.codigo = codigo;
        this.pinHash = pinHash;
        this.nombre = nombre == null ? "" : nombre;
    }

    public void actualizar(String nombre, boolean activo) {
        this.nombre = nombre == null ? "" : nombre;
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
}
