-- Esquema base de RF Inventario. En una base ya existente (creada a mano
-- antes de adoptar Flyway) esta migración se saltea por el baseline
-- configurado en application.properties; en una base nueva se ejecuta y deja
-- exactamente el mismo esquema.

CREATE TABLE IF NOT EXISTS products (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    name       VARCHAR(255) NOT NULL,
    sku        VARCHAR(128) NOT NULL,
    quantity   INT NOT NULL DEFAULT 0,
    location   VARCHAR(255) NOT NULL DEFAULT '',
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_products_sku (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS users (
    code     VARCHAR(32) NOT NULL,
    pin_hash CHAR(64) NOT NULL,
    name     VARCHAR(255) NOT NULL DEFAULT '',
    active   TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
