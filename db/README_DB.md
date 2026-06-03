# README_DB - Stockcito Database

Base de datos MySQL 8 para Stockcito. Los archivos de esta carpeta son la fuente de verdad del backend Jersey/JDBC.

## Inicio Rapido

```bash
cd db
cp .env.example .env
docker compose up -d
```

Adminer queda en `http://localhost:8081`.

## Correcciones Aplicadas

- `Adminer` fue movido de `8080:8080` a `8081:8080` para evitar conflicto con Spring Boot en `8080`.
- Los indices parciales originales `CREATE UNIQUE INDEX ... WHERE ...` no son compatibles con MySQL. Se reemplazaron por indices `UNIQUE` normales en `products.sku` e `import_jobs.file_hash`. MySQL permite multiples valores `NULL` en un indice unico, asi que conserva el comportamiento buscado.

## Conexion desde la API

La clase `org.stockcito.connection.ConexionMysql` usa estos valores por defecto:

- Host: `127.0.0.1`
- Puerto: `3306`
- Base de datos: `stockcito_db`
- Usuario: `stockcito`
- Password: `stockcito123`

Tambien puede leer variables de entorno: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.

La autenticacion usa `JWT_SECRET` para firmar tokens y `JWT_EXPIRATION_SECONDS`
para configurar su vigencia. Define un `JWT_SECRET` propio fuera de desarrollo.

Usuarios de desarrollo incluidos por `seed.sql`:

- `admin@stockcito.com` / `admin123`
- `operador@stockcito.com` / `operador123`

Si la base ya existia antes del enum `VALID/WARNING/ERROR` de importaciones,
ejecuta `db/migrate_import_items_status.sql` una vez.

Si la API corre desde Apache Tomcat/NetBeans en tu maquina, usa `DB_HOST=127.0.0.1` o no definas `DB_HOST`.
El nombre `stockcito-db` solo funciona para otros contenedores dentro de Docker Compose.

## Error Access Denied

Si aparece:

```text
Access denied for user 'stockcito'@'localhost' (using password: YES)
```

la API si encontro MySQL, pero ese servidor no acepta el usuario/password configurado.

Opcion A: si usas Docker para esta BD y no necesitas conservar datos:

```bash
cd db
docker compose down -v
docker compose up -d
```

Opcion B: si usas un MySQL local ya instalado, crea el usuario:

```bash
mysql -u root -p < db/fix_access_denied.sql
```

Despues carga el esquema y datos si todavia no existen:

```bash
mysql -u stockcito -pstockcito123 stockcito_db < db/schema.sql
mysql -u stockcito -pstockcito123 stockcito_db < db/seed.sql
```

Opcion C: si quieres usar otro usuario, configura Tomcat/NetBeans con propiedades JVM:

```text
-DDB_USER=root -DDB_PASSWORD=tu_password -DDB_NAME=stockcito_db -DDB_HOST=127.0.0.1
```
