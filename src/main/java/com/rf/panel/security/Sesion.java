package com.rf.panel.security;

/**
 * Quién está pidiendo, ya resuelto desde el token.
 *
 * `rol` distingue de qué login salió el token: {@link #ROL_PANEL} para un
 * administrador del panel web (tabla `panel_admins`), o el {@code RolOperario}
 * del operario ("OPERARIO"/"SUPERVISOR") para un token de la app Android.
 */
public record Sesion(String codigo, String nombre, String rol) {

    /** Valor de {@link #rol} para un token emitido por {@code /api/panel/auth/login}. */
    public static final String ROL_PANEL = "PANEL";

    /** Rol de operario con permisos de jefe de depósito en la app (no da acceso al panel). */
    public static final String ROL_SUPERVISOR = "SUPERVISOR";

    public boolean esPanel() {
        return ROL_PANEL.equals(rol);
    }

    /** Puede aprobar conteos, borrar productos, gestionar ubicaciones y pedidos. */
    public boolean esSupervisor() {
        return esPanel() || ROL_SUPERVISOR.equals(rol);
    }
}
