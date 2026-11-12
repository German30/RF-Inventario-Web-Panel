-- El PIN pasa de SHA-256 sin salt (rápido, pensado para integridad, no para
-- resistir fuerza bruta) a BCrypt (lento a propósito, con salt por fila).
-- CHAR(64) (hex de SHA-256) no alcanza para un hash BCrypt ($2a$10$... = 60
-- caracteres); se amplía con margen.
ALTER TABLE users MODIFY pin_hash VARCHAR(72) NOT NULL;

-- Re-hashea a BCrypt los operarios de demo sembrados por OperarioSeeder, con
-- los mismos PINs de siempre (1001/1234, 9001/4321), para no dejarlos
-- bloqueados. Cualquier OTRO operario que ya existiera en esta base queda con
-- un hash SHA-256 que ya no matchea contra nada — hay que reasignarle el PIN
-- (UPDATE users SET pin_hash = ... con un hash BCrypt nuevo).
UPDATE users SET pin_hash = '$2a$10$7iouW00273DGpOovJ34CteFaUwlXZlqqQqnXhZyvcaOgkroLSf6Ze' WHERE code = '1001';
UPDATE users SET pin_hash = '$2a$10$dWIDWUT0whbgA4ZRnQZMcOCAqNHDVjVQ2uMr/7PO2kUoecuSTVG1u' WHERE code = '9001';
