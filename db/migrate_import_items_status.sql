-- Ejecutar solo en bases creadas con el enum anterior de import_items.status.
ALTER TABLE import_items
    MODIFY status ENUM('PENDING', 'CONFIRMED', 'REJECTED', 'DUPLICATE', 'VALID', 'WARNING', 'ERROR')
    NOT NULL DEFAULT 'VALID';

UPDATE import_items SET status = 'VALID' WHERE status IN ('PENDING', 'CONFIRMED');
UPDATE import_items SET status = 'ERROR' WHERE status = 'REJECTED';
UPDATE import_items SET status = 'WARNING' WHERE status = 'DUPLICATE';

ALTER TABLE import_items
    MODIFY status ENUM('VALID', 'WARNING', 'ERROR') NOT NULL DEFAULT 'VALID';
