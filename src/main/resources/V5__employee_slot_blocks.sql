-- ============================================================
-- V5__employee_slot_blocks.sql
-- Employee management + manual slot blocking
-- MySQL 8 compatible (matches existing app.yml dialect)
-- Place in: src/main/resources/db/migration/
-- OR run manually on your Railway MySQL instance
-- ============================================================

-- ── 1. employees ─────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS employees (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shop_id         BIGINT          NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    role            VARCHAR(100)    DEFAULT NULL,
    avatar          VARCHAR(10)     NOT NULL DEFAULT '💈',
    phone           VARCHAR(20)     DEFAULT NULL,
    bio             VARCHAR(300)    DEFAULT NULL,
    specialties     VARCHAR(300)    DEFAULT NULL,
    active          TINYINT(1)      NOT NULL DEFAULT 1,
    display_order   INT             NOT NULL DEFAULT 0,
    avg_rating      DECIMAL(3,2)    NOT NULL DEFAULT 0.00,
    total_reviews   INT             NOT NULL DEFAULT 0,
    total_bookings  INT             NOT NULL DEFAULT 0,
    total_earnings  DECIMAL(12,2)   NOT NULL DEFAULT 0.00,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    CONSTRAINT fk_emp_shop FOREIGN KEY (shop_id) REFERENCES shops(id) ON DELETE CASCADE,
    INDEX idx_emp_shop   (shop_id),
    INDEX idx_emp_active (shop_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 2. slot_blocks ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS slot_blocks (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    shop_id         BIGINT          NOT NULL,
    employee_id     BIGINT          DEFAULT NULL,
    block_date      DATE            NOT NULL,
    slot_time       TIME            NOT NULL,
    reason          VARCHAR(200)    DEFAULT NULL,
    blocked_by      VARCHAR(100)    NOT NULL DEFAULT 'OWNER',
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    CONSTRAINT fk_sb_shop FOREIGN KEY (shop_id)     REFERENCES shops(id)     ON DELETE CASCADE,
    CONSTRAINT fk_sb_emp  FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    INDEX idx_sb_shop_date (shop_id, block_date),
    INDEX idx_sb_employee  (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── 3. bookings — add employee columns ───────────────────────────────────
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS employee_id        BIGINT       DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS employee_snapshot  VARCHAR(100) DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS employee_rating    INT          DEFAULT NULL;

-- Add FK only if not already present (safe to re-run)
ALTER TABLE bookings
    ADD CONSTRAINT fk_bk_employee
    FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE SET NULL;

ALTER TABLE bookings ADD INDEX idx_bk_employee (employee_id);
