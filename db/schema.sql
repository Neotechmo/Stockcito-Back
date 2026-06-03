-- ============================================================
-- STOCKCITO - Schema SQL Inicial
-- MySQL 8.0+
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS users (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)    NOT NULL,
    email         VARCHAR(150)    NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    role          ENUM('ADMIN', 'OPERADOR') NOT NULL DEFAULT 'OPERADOR',
    is_active     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);

CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL,
    description TEXT,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS suppliers (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    name         VARCHAR(150)    NOT NULL,
    contact_name VARCHAR(100),
    email        VARCHAR(150),
    phone        VARCHAR(30),
    address      TEXT,
    notes        TEXT,
    is_active    BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_suppliers PRIMARY KEY (id)
);

CREATE INDEX idx_suppliers_name ON suppliers (name);

CREATE TABLE IF NOT EXISTS products (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    category_id     BIGINT          NOT NULL,
    supplier_id     BIGINT,
    name            VARCHAR(200)    NOT NULL,
    sku             VARCHAR(100),
    description     TEXT,
    unit_of_measure VARCHAR(50)     NOT NULL DEFAULT 'pieza',
    min_stock       DECIMAL(10,2)   NOT NULL DEFAULT 0,
    current_stock   DECIMAL(10,2)   NOT NULL DEFAULT 0,
    unit_price      DECIMAL(12,2),
    barcode         VARCHAR(100),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id)
);

-- MySQL permite multiples NULL en indices UNIQUE; no requiere indice parcial.
CREATE UNIQUE INDEX idx_products_sku ON products (sku);
CREATE INDEX idx_products_name ON products (name);
CREATE INDEX idx_products_cat ON products (category_id);
CREATE INDEX idx_products_sup ON products (supplier_id);
CREATE INDEX idx_products_barcode ON products (barcode);

CREATE TABLE IF NOT EXISTS inventory_movements (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    product_id      BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    movement_type   ENUM('ENTRY', 'EXIT') NOT NULL,
    quantity        DECIMAL(10,2)   NOT NULL,
    stock_before    DECIMAL(10,2)   NOT NULL,
    stock_after     DECIMAL(10,2)   NOT NULL,
    movement_date   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference_id    BIGINT,
    notes           TEXT,
    original_text   TEXT,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_inventory_movements PRIMARY KEY (id),
    CONSTRAINT fk_inv_mov_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_inv_mov_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_inv_mov_qty CHECK (quantity > 0)
);

CREATE INDEX idx_inv_mov_product ON inventory_movements (product_id);
CREATE INDEX idx_inv_mov_user ON inventory_movements (user_id);
CREATE INDEX idx_inv_mov_date ON inventory_movements (movement_date);
CREATE INDEX idx_inv_mov_type ON inventory_movements (movement_type);

CREATE TABLE IF NOT EXISTS purchase_requests (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    product_id      BIGINT          NOT NULL,
    requested_by    BIGINT          NOT NULL,
    approved_by     BIGINT,
    quantity        DECIMAL(10,2)   NOT NULL,
    status          ENUM('PENDING', 'PURCHASED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    notes           TEXT,
    requested_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_purchase_requests PRIMARY KEY (id),
    CONSTRAINT fk_pr_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_pr_requested_by FOREIGN KEY (requested_by) REFERENCES users (id),
    CONSTRAINT fk_pr_approved_by FOREIGN KEY (approved_by) REFERENCES users (id),
    CONSTRAINT chk_pr_qty CHECK (quantity > 0)
);

CREATE INDEX idx_pr_product ON purchase_requests (product_id);
CREATE INDEX idx_pr_status ON purchase_requests (status);
CREATE INDEX idx_pr_requester ON purchase_requests (requested_by);

CREATE TABLE IF NOT EXISTS import_jobs (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    user_id         BIGINT          NOT NULL,
    source_type     ENUM('PDF', 'IMAGE', 'CSV', 'MANUAL') NOT NULL,
    source_filename VARCHAR(255),
    file_hash       VARCHAR(64),
    status          ENUM('PENDING', 'PROCESSING', 'PREVIEW', 'CONFIRMED', 'FAILED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    raw_text        LONGTEXT,
    ai_model        VARCHAR(100),
    notes           TEXT,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at    DATETIME,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_import_jobs PRIMARY KEY (id),
    CONSTRAINT fk_ij_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_ij_user ON import_jobs (user_id);
CREATE INDEX idx_ij_status ON import_jobs (status);
-- MySQL permite multiples NULL en indices UNIQUE; no requiere indice parcial.
CREATE UNIQUE INDEX idx_ij_file_hash ON import_jobs (file_hash);

CREATE TABLE IF NOT EXISTS import_items (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    import_job_id   BIGINT          NOT NULL,
    product_id      BIGINT,
    product_name    VARCHAR(200)    NOT NULL,
    movement_type   ENUM('ENTRY', 'EXIT') NOT NULL,
    quantity        DECIMAL(10,2)   NOT NULL,
    unit_of_measure VARCHAR(50),
    confidence      DECIMAL(5,4),
    raw_line        TEXT,
    status          ENUM('PENDING', 'CONFIRMED', 'REJECTED', 'DUPLICATE') NOT NULL DEFAULT 'PENDING',
    duplicate_of    BIGINT,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_import_items PRIMARY KEY (id),
    CONSTRAINT fk_ii_job FOREIGN KEY (import_job_id) REFERENCES import_jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_ii_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_ii_duplicate FOREIGN KEY (duplicate_of) REFERENCES inventory_movements (id),
    CONSTRAINT chk_ii_qty CHECK (quantity > 0),
    CONSTRAINT chk_ii_confidence CHECK (confidence IS NULL OR (confidence >= 0 AND confidence <= 1))
);

CREATE INDEX idx_ii_job ON import_items (import_job_id);
CREATE INDEX idx_ii_product ON import_items (product_id);
CREATE INDEX idx_ii_status ON import_items (status);

SET FOREIGN_KEY_CHECKS = 1;
