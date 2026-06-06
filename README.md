# Stockcito Backend

API REST para administrar usuarios, productos, proveedores, categorías,
inventario, solicitudes de compra e importaciones de movimientos.

El proyecto utiliza Java 21, Jakarta REST/Jersey, JDBC, MySQL 8, Gson, BCrypt,
JWT, Apache Tomcat y NetBeans/Ant.

## Cómo funciona

La aplicación conserva la estructura original del proyecto:

```text
src/java/org/stockcito/
├── connection/   Conexión JDBC a MySQL
├── controller/   Consultas SQL y reglas de negocio
├── model/        Modelos enviados y recibidos como JSON
└── rest/         Endpoints HTTP de Jersey

db/
├── schema.sql    Creación de tablas
├── seed.sql      Datos iniciales
└── docker-compose.yml
```

Jersey busca automáticamente los recursos dentro de `org.stockcito.rest`.
Todos los endpoints se publican bajo `/api/*`, de acuerdo con
`web/WEB-INF/web.xml`.

Los endpoints protegidos requieren un token JWT. El token se obtiene mediante
login y debe enviarse en el header:

```http
Authorization: Bearer TU_TOKEN_JWT
```

No requieren token:

```text
/api/v1/auth/*
/api/v1/health
/api/swagger-ui
/api/v3/api-docs
```

## Requisitos

- Java JDK 21
- Apache Tomcat compatible con Jakarta EE
- NetBeans con soporte para proyectos web Ant
- Docker Desktop, o una instalación local de MySQL 8

## Iniciar la base de datos

Desde la raíz del proyecto:

```bash
docker compose -f db/docker-compose.yml up -d
```

Esto inicia:

- MySQL: `localhost:3306`
- Adminer: [http://localhost:8081](http://localhost:8081)

Conexión predeterminada:

```text
Servidor:  stockcito-db (desde Adminer) o 127.0.0.1
Usuario:   stockcito
Password:  stockcito123
Base:      stockcito_db
```

Si la base ya existía antes del enum de importaciones
`VALID/WARNING/ERROR`, ejecutar una vez:

```bash
mysql -u stockcito -pstockcito123 stockcito_db < db/migrate_import_items_status.sql
```

## Ejecutar la API

1. Abrir el proyecto en NetBeans.
2. Configurar Apache Tomcat como servidor.
3. Ejecutar **Clean and Build**.
4. Ejecutar **Run** para desplegar `Stockcito.war`.

La URL base predeterminada es:

```text
http://localhost:8080/Stockcito/api
```

Comprobar que la API está activa:

```text
GET http://localhost:8080/Stockcito/api/v1/health
```

Respuesta esperada:

```json
{
  "service": "stockcito-api",
  "status": "UP"
}
```

Si Tomcat despliega el WAR con otro nombre, reemplazar `Stockcito` en todas
las URLs por el contexto utilizado por Tomcat.

## Swagger

Abrir Swagger UI en el navegador:

[http://localhost:8080/Stockcito/api/swagger-ui](http://localhost:8080/Stockcito/api/swagger-ui)

El documento OpenAPI JSON está disponible en:

[http://localhost:8080/Stockcito/api/v3/api-docs](http://localhost:8080/Stockcito/api/v3/api-docs)

Swagger UI carga sus archivos visuales desde `unpkg.com`, por lo que el
navegador necesita acceso a internet para mostrar la interfaz.

## Usuarios de prueba

Los siguientes usuarios son creados por `db/seed.sql`:

```text
Administrador
Email:    admin@stockcito.com
Password: admin123
Role:     ADMIN

Operador
Email:    operador@stockcito.com
Password: operador123
Role:     OPERADOR
```

## Usar la API en Postman o Yaak

### 1. Crear un entorno

Crear una variable llamada `baseUrl`:

```text
http://localhost:8080/Stockcito/api
```

En Postman puede usarse como `{{baseUrl}}`. En Yaak puede guardarse como una
variable de entorno con el mismo nombre.

### 2. Iniciar sesión

Crear una petición:

```http
POST {{baseUrl}}/v1/auth/login
Content-Type: application/json
```

Body JSON:

```json
{
  "email": "admin@stockcito.com",
  "password": "admin123"
}
```

La respuesta contiene:

```json
{
  "token": "jwt-token",
  "user": {
    "id": 1,
    "name": "Administrador",
    "email": "admin@stockcito.com",
    "role": "ADMIN"
  }
}
```

Guardar el valor de `token` en una variable llamada `token`.

### 3. Configurar autenticación

Para las peticiones protegidas, elegir autenticación **Bearer Token** y usar:

```text
{{token}}
```

También puede agregarse manualmente:

```http
Authorization: Bearer {{token}}
Content-Type: application/json
```

### 4. Importar el OpenAPI

Postman:

1. Seleccionar **Import**.
2. Elegir **Link**.
3. Pegar `http://localhost:8080/Stockcito/api/v3/api-docs`.

Yaak:

1. Seleccionar **Import**.
2. Elegir OpenAPI desde URL.
3. Pegar `http://localhost:8080/Stockcito/api/v3/api-docs`.

Después de importar, configurar el Bearer Token en las peticiones protegidas.

## Ejemplos de peticiones

### Registrar operador

```http
POST {{baseUrl}}/v1/auth/register
Content-Type: application/json
```

```json
{
  "name": "Nuevo Operador",
  "email": "nuevo@stockcito.com",
  "password": "password123"
}
```

El registro público siempre crea usuarios con role `OPERADOR`.

### Listar productos

```http
GET {{baseUrl}}/v1/products
Authorization: Bearer {{token}}
```

### Crear categoría

```http
POST {{baseUrl}}/v1/categories
Authorization: Bearer {{token}}
Content-Type: application/json
```

```json
{
  "name": "Congelados",
  "description": "Productos almacenados en congelación"
}
```

### Crear movimiento de inventario

```http
POST {{baseUrl}}/v1/inventory-movements
Authorization: Bearer {{token}}
Content-Type: application/json
```

```json
{
  "productId": 1,
  "userId": 1,
  "movementType": "ENTRY",
  "quantity": 10,
  "notes": "Entrada desde proveedor"
}
```

`movementType` admite `ENTRY` y `EXIT`. El stock del producto se actualiza
automáticamente. Una salida mayor al stock disponible es rechazada.

### Consultar inventario actual

```http
GET {{baseUrl}}/v1/inventory
Authorization: Bearer {{token}}
```

### Crear solicitud de compra

```http
POST {{baseUrl}}/v1/purchase-requests
Authorization: Bearer {{token}}
Content-Type: application/json
```

```json
{
  "productId": 1,
  "requestedBy": 2,
  "quantity": 20,
  "status": "PENDING",
  "notes": "Reposición semanal"
}
```

Filtrar solicitudes pendientes:

```http
GET {{baseUrl}}/v1/purchase-requests?status=PENDING
Authorization: Bearer {{token}}
```

Actualizar su estado:

```http
PATCH {{baseUrl}}/v1/purchase-requests/1/status
Authorization: Bearer {{token}}
Content-Type: application/json
```

```json
{
  "status": "PURCHASED",
  "approvedBy": 1
}
```

### Previsualizar y confirmar una importación

Crear preview:

```http
POST {{baseUrl}}/v1/imports/preview
Authorization: Bearer {{token}}
Content-Type: application/json
```

```json
{
  "userId": 1,
  "sourceType": "MANUAL",
  "sourceFilename": "entrada-manual",
  "notes": "Importación de prueba",
  "items": [
    {
      "productId": 1,
      "productName": "Arroz Blanco 1 kg",
      "movementType": "ENTRY",
      "quantity": 5,
      "unitOfMeasure": "kg",
      "confidence": 1,
      "rawLine": "Arroz Blanco 1 kg: 5"
    }
  ]
}
```

Confirmar el preview y crear movimientos para sus items `VALID`:

```http
POST {{baseUrl}}/v1/imports/1/confirm
Authorization: Bearer {{token}}
```

Cancelar un preview:

```http
POST {{baseUrl}}/v1/imports/1/cancel
Authorization: Bearer {{token}}
```

## Endpoints principales

| Módulo | Rutas |
|---|---|
| Health | `GET /v1/health` |
| Auth | `POST /v1/auth/login`, `POST /v1/auth/register` |
| Usuarios | CRUD `/v1/users` |
| Categorías | CRUD `/v1/categories` |
| Proveedores | CRUD `/v1/suppliers` |
| Productos | CRUD `/v1/products` |
| Movimientos | CRUD `/v1/inventory-movements` |
| Inventario | `GET /v1/inventory` |
| Solicitudes | CRUD `/v1/purchase-requests`, `PATCH /{id}/status` |
| Importaciones | `GET /v1/imports`, preview, confirm y cancel |
| AI | `POST /v1/ai/summaries` |

Todas las rutas de la tabla deben agregarse después de:

```text
http://localhost:8080/Stockcito/api
```

## Configuración

La conexión, JWT y OpenAI pueden configurarse mediante variables de entorno o
mediante un archivo `.env` en la raíz del proyecto:

```text
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION_SECONDS
OPENAI_API_KEY
OPENAI_MODEL
```

Para desarrollo local con NetBeans:

```bash
cp .env.example .env
```

Editar `.env` y colocar los valores reales. El archivo `.env` no debe subirse
al repositorio porque contiene secretos.

La prioridad de configuración es:

```text
1. Propiedades JVM, por ejemplo -DOPENAI_API_KEY=...
2. Variables de entorno del sistema
3. Archivo .env
4. Valores predeterminados de desarrollo
```

Si NetBeans/Tomcat se ejecuta desde otra carpeta y no encuentra `.env`, puede
indicarse la ruta exacta con:

```text
-DSTOCKCITO_ENV_FILE=/ruta/completa/al/proyecto/.env
```

Para cualquier despliegue real es obligatorio definir un `JWT_SECRET` privado.

## Alertas De Inventario Con GPT

La API incluye un endpoint protegido que consulta la base de datos y usa OpenAI
para generar recomendaciones operativas de inventario. No se le manda un texto
libre: Stockcito arma el contexto con productos bajo mínimo, productos con
muchas salidas y productos con pocas o nulas salidas recientes.

```http
POST {{baseUrl}}/v1/ai/summaries
Authorization: Bearer {{token}}
Content-Type: application/json
```

Body opcional:

```json
{
  "daysBack": 30,
  "lowStockLimit": 8,
  "highExitLimit": 8,
  "slowMovementDays": 60,
  "slowMovementLimit": 8,
  "maxSentences": 5,
  "language": "español"
}
```

También puede enviarse `{}` para usar los valores predeterminados.

Respuesta:

```json
{
  "summary": "Hay stock bajo de Arroz Blanco: quedan 2 kg y el minimo es 10 kg, conviene comprar mas. Bebidas tuvo muchas salidas en los ultimos 30 dias, revisa proveedor y reposicion. Papel bond no registra salidas recientes, revisa bodega para evitar caducidad o sobreinventario.",
  "model": "gpt-4.1-mini"
}
```

Configuración requerida:

```text
OPENAI_API_KEY=tu_api_key
OPENAI_MODEL=gpt-4.1-mini
```

`OPENAI_MODEL` es opcional; si no se define, usa `gpt-4.1-mini`.

## Despliegue Docker En AWS

El despliegue recomendado para producción ligera es Docker en EC2 con RDS MySQL
y NGINX/HTTPS.

Archivos principales:

- `Dockerfile`: construye `Stockcito.war` y lo corre en Tomcat 11.
- `docker-compose.prod.yml`: levanta el contenedor de la API en EC2.
- `.env.prod.example`: plantilla de variables para RDS y JWT.
- `deploy/aws-ec2-docker.md`: guía paso a paso para AWS.

Comando base en EC2:

```bash
cp .env.prod.example .env.prod
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

## Solución de problemas

### `401 Token JWT ausente o invalido`

Iniciar sesión nuevamente y enviar el token como Bearer Token. Los tokens
expiran después de 24 horas por defecto.

### `Access denied for user 'stockcito'`

Recrear la base Docker si no se necesitan conservar datos:

```bash
docker compose -f db/docker-compose.yml down -v
docker compose -f db/docker-compose.yml up -d
```

### Swagger no abre

Verificar primero:

```text
http://localhost:8080/Stockcito/api/v1/health
http://localhost:8080/Stockcito/api/v3/api-docs
```

Si funcionan pero Swagger UI aparece vacío, revisar la conexión a internet,
porque sus archivos CSS y JavaScript se cargan desde `unpkg.com`.
