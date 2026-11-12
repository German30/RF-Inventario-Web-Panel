-- El login del panel web se separa del de la app. Hasta acá ambos salían de
-- `users`: un operario con role='SUPERVISOR' podía entrar al panel. Desde
-- ahora son dos identidades SIN solape:
--   users        -> operarios de la app Android   (POST /api/auth/login)
--   panel_admins -> administradores del panel web (POST /api/panel/auth/login)
-- Misma forma de credencial: código numérico + PIN con BCrypt.
CREATE TABLE IF NOT EXISTS panel_admins (
    code     VARCHAR(32)  NOT NULL,
    pin_hash VARCHAR(72)  NOT NULL,
    name     VARCHAR(255) NOT NULL DEFAULT '',
    active   TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Los supervisores que había en `users` PASAN a `panel_admins` con su mismo
-- código, nombre y hash de PIN, y se BORRAN de `users`: una cuenta es de la app
-- o del panel, nunca de las dos. `stock_movements` guarda el código y el
-- nombre del operario desnormalizados, así que el historial no se rompe al
-- sacar la fila. En una base nueva y vacía `users` todavía no tiene filas y
-- esto no mueve nada: AdministradorSeeder crea el primer administrador.
INSERT INTO panel_admins (code, pin_hash, name, active)
SELECT code, pin_hash, name, active FROM users WHERE role = 'SUPERVISOR'
ON DUPLICATE KEY UPDATE name = VALUES(name);

DELETE FROM users WHERE role = 'SUPERVISOR';

-- `users.role` se conserva (distinción interna de la app), pero ya no concede
-- acceso al panel y a partir de acá solo debería valer 'OPERARIO'.
