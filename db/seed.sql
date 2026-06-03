-- ============================================================
-- STOCKCITO - Datos Seed Iniciales
-- Ejecutar DESPUES de schema.sql
-- ============================================================

INSERT INTO users (name, email, password_hash, role) VALUES
    ('Administrador', 'admin@stockcito.com', '$2a$12$VU8LO0jvCV8UksF1E/yEPuJ5LKYZx7NyR46oFe9dfgtlpzgU0ydeu', 'ADMIN'),
    ('Operador Demo', 'operador@stockcito.com', '$2a$12$svShiuCMAKyKhL50aKHKiOjP7/8vyrA8FRqF8TiMuHpaGPUNnjkIG', 'OPERADOR');

INSERT INTO categories (name, description) VALUES
    ('Alimentos Secos', 'Granos, harinas, azucares y productos no perecederos'),
    ('Lacteos', 'Leche, queso, mantequilla y derivados lacteos'),
    ('Carnes y Embutidos', 'Carnes frescas, congeladas y embutidos'),
    ('Bebidas', 'Agua, refrescos, jugos y bebidas calientes'),
    ('Limpieza', 'Detergentes, desinfectantes y articulos de limpieza'),
    ('Papeleria', 'Papeleria, impresion y materiales de oficina'),
    ('Electronica', 'Cables, pilas y accesorios electronicos'),
    ('Herramientas', 'Herramientas de mano y equipos menores');

INSERT INTO suppliers (name, contact_name, email, phone, notes) VALUES
    ('Distribuidora Central S.A.', 'Carlos Mendez', 'ventas@distcentral.com', '+52 477 100 0001', 'Proveedor principal de abarrotes'),
    ('Lacteos del Bajio', 'Maria Rodriguez', 'pedidos@lacteosbajio.mx', '+52 477 200 0002', 'Entrega lunes y jueves'),
    ('Carnes Premium GTO', 'Roberto Sanchez', 'r.sanchez@carnespremium.mx', '+52 477 300 0003', 'Pedido minimo 50 kg'),
    ('Bebidas y Mas', 'Ana Flores', 'ana@bebidasmas.com.mx', '+52 477 400 0004', NULL),
    ('Limpieza Total', 'Jose Garcia', 'ventas@limpiezatotal.mx', '+52 477 500 0005', 'Facturacion a 30 dias');

INSERT INTO products (category_id, supplier_id, name, sku, unit_of_measure, min_stock, current_stock, unit_price) VALUES
    (1, 1, 'Arroz Blanco 1 kg', 'ARR-001', 'kg', 10.00, 45.00, 18.50),
    (1, 1, 'Frijol Negro 1 kg', 'FRJ-001', 'kg', 8.00, 30.00, 22.00),
    (1, 1, 'Azucar Refinada 1 kg', 'AZU-001', 'kg', 12.00, 60.00, 16.00),
    (1, 1, 'Harina de Trigo 1 kg', 'HAR-001', 'kg', 10.00, 25.00, 14.50),
    (1, 1, 'Aceite Vegetal 1 L', 'ACE-001', 'litro', 6.00, 18.00, 28.00),
    (2, 2, 'Leche Entera 1 L', 'LEC-001', 'litro', 24.00, 120.00, 12.00),
    (2, 2, 'Queso Fresco 400 g', 'QUE-001', 'pieza', 5.00, 20.00, 45.00),
    (2, 2, 'Mantequilla 90 g', 'MAN-001', 'pieza', 6.00, 15.00, 22.00),
    (3, 3, 'Pechuga de Pollo', 'POL-001', 'kg', 5.00, 12.00, 130.00),
    (3, 3, 'Carne Molida de Res', 'RES-001', 'kg', 3.00, 8.00, 180.00),
    (4, 4, 'Agua Natural 600 ml', 'AGU-001', 'pieza', 24.00, 144.00, 8.00),
    (4, 4, 'Refresco Cola 600 ml', 'REF-001', 'pieza', 12.00, 60.00, 14.00),
    (5, 5, 'Cloro 1 L', 'CLO-001', 'litro', 4.00, 10.00, 18.00),
    (5, 5, 'Detergente en Polvo 1 kg', 'DET-001', 'kg', 3.00, 8.00, 35.00);

INSERT INTO inventory_movements (product_id, user_id, movement_type, quantity, stock_before, stock_after, notes) VALUES
    (1, 1, 'ENTRY', 45.00, 0.00, 45.00, 'Carga inicial de inventario'),
    (2, 1, 'ENTRY', 30.00, 0.00, 30.00, 'Carga inicial de inventario'),
    (3, 1, 'ENTRY', 60.00, 0.00, 60.00, 'Carga inicial de inventario'),
    (4, 1, 'ENTRY', 25.00, 0.00, 25.00, 'Carga inicial de inventario'),
    (5, 1, 'ENTRY', 18.00, 0.00, 18.00, 'Carga inicial de inventario'),
    (6, 1, 'ENTRY', 120.00, 0.00, 120.00, 'Carga inicial de inventario'),
    (7, 1, 'ENTRY', 20.00, 0.00, 20.00, 'Carga inicial de inventario'),
    (8, 1, 'ENTRY', 15.00, 0.00, 15.00, 'Carga inicial de inventario'),
    (9, 1, 'ENTRY', 12.00, 0.00, 12.00, 'Carga inicial de inventario'),
    (10, 1, 'ENTRY', 8.00, 0.00, 8.00, 'Carga inicial de inventario'),
    (11, 1, 'ENTRY', 144.00, 0.00, 144.00, 'Carga inicial de inventario'),
    (12, 1, 'ENTRY', 60.00, 0.00, 60.00, 'Carga inicial de inventario'),
    (13, 1, 'ENTRY', 10.00, 0.00, 10.00, 'Carga inicial de inventario'),
    (14, 1, 'ENTRY', 8.00, 0.00, 8.00, 'Carga inicial de inventario');

INSERT INTO purchase_requests (product_id, requested_by, quantity, status, notes) VALUES
    (9, 2, 10.00, 'PENDING', 'Stock bajo, necesario para la semana'),
    (10, 2, 5.00, 'PENDING', 'Reposicion mensual'),
    (3, 2, 25.00, 'PURCHASED', 'Comprado en Distribuidora Central');
