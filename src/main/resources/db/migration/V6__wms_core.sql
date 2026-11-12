-- V6: de "lista de productos" a inventario con ubicaciones, lotes y operaciones.
--
--   * products gana ficha completa (categoría, unidad, stock mínimo, costo,
--     control por lote). `quantity` pasa a ser el TOTAL (suma de stock_levels)
--     y `location` la ubicación PRINCIPAL (donde caen los ajustes sin ubicación).
--   * locations: ubicaciones físicas con código escaneable.
--   * stock_levels: cuánto hay de cada producto en cada ubicación y lote.
--   * product_barcodes: códigos alternativos (EAN del proveedor, código interno).
--   * processed_operations: idempotencia para la cola offline de la app.
--   * count_sessions / count_lines: conteo cíclico.
--   * orders / order_lines: recepción (COMPRA) y picking (VENTA).

ALTER TABLE products
    ADD COLUMN description VARCHAR(1000) NOT NULL DEFAULT '',
    ADD COLUMN category    VARCHAR(100)  NOT NULL DEFAULT '',
    ADD COLUMN unit        VARCHAR(20)   NOT NULL DEFAULT 'UN',
    ADD COLUMN min_stock   INT           NOT NULL DEFAULT 0,
    ADD COLUMN unit_cost   DECIMAL(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN tracks_lots TINYINT(1)    NOT NULL DEFAULT 0,
    ADD KEY idx_products_updated (updated_at);

CREATE TABLE IF NOT EXISTS product_barcodes (
    code       VARCHAR(128) NOT NULL,
    product_id BIGINT       NOT NULL,
    PRIMARY KEY (code),
    KEY idx_product_barcodes_product (product_id),
    CONSTRAINT fk_product_barcodes_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS locations (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(128) NOT NULL,
    description VARCHAR(255) NOT NULL DEFAULT '',
    zone        VARCHAR(64)  NOT NULL DEFAULT '',
    active      TINYINT(1)   NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_locations_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS stock_levels (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    product_id  BIGINT      NOT NULL,
    location_id BIGINT      NOT NULL,
    lot         VARCHAR(64) NOT NULL DEFAULT '',
    expires_on  DATE        NULL,
    quantity    INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_stock_levels (product_id, location_id, lot),
    KEY idx_stock_levels_location (location_id),
    KEY idx_stock_levels_expiry (expires_on),
    CONSTRAINT fk_stock_levels_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_levels_location FOREIGN KEY (location_id) REFERENCES locations (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Migración de datos: cada texto de `products.location` se vuelve una
-- ubicación real y el stock existente queda asignado a ella (sin lote).
UPDATE products SET location = LEFT(UPPER(TRIM(location)), 128);
UPDATE products SET location = 'GENERAL' WHERE location = '';

INSERT IGNORE INTO locations (code, description) VALUES ('GENERAL', 'Ubicación por defecto');
INSERT IGNORE INTO locations (code) SELECT DISTINCT location FROM products;

INSERT INTO stock_levels (product_id, location_id, lot, quantity)
SELECT p.id, l.id, '', p.quantity
FROM products p JOIN locations l ON l.code = p.location
WHERE p.quantity > 0;

ALTER TABLE stock_movements
    ADD COLUMN reason           VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN reference        VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN location_code    VARCHAR(128) NOT NULL DEFAULT '',
    ADD COLUMN to_location_code VARCHAR(128) NOT NULL DEFAULT '',
    ADD COLUMN lot              VARCHAR(64)  NOT NULL DEFAULT '',
    ADD KEY idx_stock_movements_created (created_at);

CREATE TABLE IF NOT EXISTS processed_operations (
    client_op_id  VARCHAR(64) NOT NULL,
    operator_code VARCHAR(32) NOT NULL,
    created_at    BIGINT      NOT NULL,
    PRIMARY KEY (client_op_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS count_sessions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    location_code   VARCHAR(128) NOT NULL DEFAULT '',
    blind           TINYINT(1)   NOT NULL DEFAULT 1,
    status          VARCHAR(20)  NOT NULL,
    note            VARCHAR(500) NOT NULL DEFAULT '',
    created_by_code VARCHAR(32)  NOT NULL,
    created_by_name VARCHAR(255) NOT NULL DEFAULT '',
    created_at      BIGINT       NOT NULL,
    closed_at       BIGINT       NULL,
    applied_at      BIGINT       NULL,
    applied_by      VARCHAR(32)  NULL,
    PRIMARY KEY (id),
    KEY idx_count_sessions_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS count_lines (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    session_id    BIGINT       NOT NULL,
    product_id    BIGINT       NOT NULL,
    sku           VARCHAR(128) NOT NULL,
    name          VARCHAR(255) NOT NULL DEFAULT '',
    location_code VARCHAR(128) NOT NULL,
    lot           VARCHAR(64)  NOT NULL DEFAULT '',
    expected_qty  INT          NOT NULL DEFAULT 0,
    counted_qty   INT          NULL,
    counted_by    VARCHAR(32)  NULL,
    counted_at    BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_count_lines (session_id, product_id, location_code, lot),
    CONSTRAINT fk_count_lines_session FOREIGN KEY (session_id) REFERENCES count_sessions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    type         VARCHAR(10)  NOT NULL,
    number       VARCHAR(64)  NOT NULL,
    party        VARCHAR(255) NOT NULL DEFAULT '',
    status       VARCHAR(20)  NOT NULL,
    note         VARCHAR(500) NOT NULL DEFAULT '',
    created_by   VARCHAR(32)  NOT NULL,
    created_at   BIGINT       NOT NULL,
    completed_at BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_orders_number (number),
    KEY idx_orders_status (type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS order_lines (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    order_id    BIGINT       NOT NULL,
    product_id  BIGINT       NOT NULL,
    sku         VARCHAR(128) NOT NULL,
    name        VARCHAR(255) NOT NULL DEFAULT '',
    qty_ordered INT          NOT NULL,
    qty_done    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order_lines_order (order_id),
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
