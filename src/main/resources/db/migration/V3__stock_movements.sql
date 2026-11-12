-- Historial de movimientos: responde "quién tocó este producto y cuándo".
-- Denormaliza sku/operador a propósito: la fila tiene que seguir teniendo
-- sentido aunque el producto se borre o el operario cambie de nombre después.
CREATE TABLE IF NOT EXISTS stock_movements (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    product_id     BIGINT NOT NULL,
    sku            VARCHAR(128) NOT NULL,
    operator_code  VARCHAR(32) NOT NULL,
    operator_name  VARCHAR(255) NOT NULL DEFAULT '',
    action         VARCHAR(20) NOT NULL,
    delta          INT NOT NULL DEFAULT 0,
    quantity_after INT NOT NULL,
    created_at     BIGINT NOT NULL,
    PRIMARY KEY (id),
    KEY idx_stock_movements_product (product_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
