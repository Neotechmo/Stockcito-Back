-- Ejecutar con un usuario administrador de MySQL, por ejemplo root.
-- Corrige: Access denied for user 'stockcito'@'localhost'

CREATE DATABASE IF NOT EXISTS stockcito_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'stockcito'@'localhost' IDENTIFIED BY 'stockcito123';
ALTER USER 'stockcito'@'localhost' IDENTIFIED BY 'stockcito123';
GRANT ALL PRIVILEGES ON stockcito_db.* TO 'stockcito'@'localhost';

CREATE USER IF NOT EXISTS 'stockcito'@'%' IDENTIFIED BY 'stockcito123';
ALTER USER 'stockcito'@'%' IDENTIFIED BY 'stockcito123';
GRANT ALL PRIVILEGES ON stockcito_db.* TO 'stockcito'@'%';

FLUSH PRIVILEGES;
