package com.rf.panel.entity;

/**
 * Rol del operario dentro de la app Android. Ya NO decide el acceso al panel
 * web: eso es cuenta aparte (tabla `panel_admins`, ver {@code V5}). Se conserva
 * SUPERVISOR como distinción interna de la app; no concede nada en este backend.
 */
public enum RolOperario {
    OPERARIO,
    SUPERVISOR
}
