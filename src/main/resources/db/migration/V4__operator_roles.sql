-- El panel web es solo para supervisores; los operarios de piso siguen
-- entrando únicamente desde la app Android. `role` es la fuente de verdad
-- que consulta el backend (JWT + @PreAuthorize), no una convención de la UI.
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'OPERARIO';

-- El operario 9001, sembrado como "Supervisor" antes de que existiera este
-- campo, es el primer supervisor real.
UPDATE users SET role = 'SUPERVISOR' WHERE code = '9001';
