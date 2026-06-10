# Despliegue Dockerizado En AWS EC2

Este despliegue corre la API en un contenedor Tomcat 11 y conecta contra RDS
MySQL. La base de datos no se corre en Docker en producción.

## 1. Preparar EC2

Usar Amazon Linux 2023 y abrir en el security group:

- `22` sólo para tu IP.
- `80` público.
- `443` público.

Instalar Docker, NGINX, Certbot y cliente MySQL:

```bash
sudo dnf update -y
sudo dnf install -y docker nginx certbot python3-certbot-nginx mysql
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
```

Cerrar sesión y volver a entrar para que aplique el grupo `docker`.

## 2. Crear RDS MySQL

Crear RDS MySQL 8 con:

- Base: `stockcito_db`
- Usuario: `stockcito`
- Acceso público: no
- Security group: permitir `3306` sólo desde el security group de la EC2.

Inicializar la base desde la EC2:

```bash
mysql -h <endpoint-rds> -u stockcito -p stockcito_db < db/schema.sql
mysql -h <endpoint-rds> -u stockcito -p stockcito_db < db/seed.sql
```

Si la base ya existía antes del enum `VALID/WARNING/ERROR`:

```bash
mysql -h <endpoint-rds> -u stockcito -p stockcito_db < db/migrate_import_items_status.sql
```

## 3. Configurar Variables

Crear `.env.prod` en la raíz del repo dentro de la EC2:

```bash
cp .env.prod.example .env.prod
nano .env.prod
```

Valores requeridos:

```text
DB_HOST=<endpoint-rds>
DB_PORT=3306
DB_NAME=stockcito_db
DB_USER=stockcito
DB_PASSWORD=<password-segura>
JWT_SECRET=<clave-larga-segura>
JWT_EXPIRATION_SECONDS=86400
OPENAI_API_KEY=<api-key-openai>
OPENAI_MODEL=gpt-4.1-mini
```

`DB_HOST` debe ser el endpoint real de RDS, por ejemplo:

```text
stockcito-db.xxxxxxxxxxxx.us-east-1.rds.amazonaws.com
```

No usar nombres de contenedores como `db-stockcito-db-1` en producción. Ese
nombre sólo existe dentro de una red Docker local y la API de producción no lo
puede resolver.

No subir `.env.prod` al repositorio.

## 4. Construir Y Levantar La API

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
docker ps
docker logs stockcito-api
```

La API queda escuchando sólo localmente en EC2:

```text
http://127.0.0.1:8080/Stockcito/api
```

## 5. Configurar Dominio Y HTTPS

Apuntar `api.tu-dominio.com` al Elastic IP de la EC2.

Copiar la plantilla HTTP de NGINX:

```bash
sudo cp deploy/nginx/stockcito.conf /etc/nginx/conf.d/stockcito.conf
sudo sed -i 's/api.tu-dominio.com/tu-dominio-real.com/g' /etc/nginx/conf.d/stockcito.conf
sudo nginx -t
sudo systemctl enable --now nginx
```

Activar HTTPS con Certbot. Certbot actualiza el bloque de NGINX y puede
configurar la redirección HTTP -> HTTPS automáticamente:

```bash
sudo certbot --nginx -d tu-dominio-real.com
sudo systemctl reload nginx
```

## 6. Validar

```bash
curl https://tu-dominio-real.com/Stockcito/api/v1/health
curl https://tu-dominio-real.com/Stockcito/api/v3/api-docs
```

Abrir Swagger:

```text
https://tu-dominio-real.com/Stockcito/api/swagger-ui
```

Probar login con usuario seed:

```json
{
  "email": "admin@stockcito.com",
  "password": "admin123"
}
```

## 7. Actualizar Una Nueva Versión

```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
docker logs stockcito-api
```

## 8. Troubleshooting

### `UnknownHostException: db-stockcito-db-1`

Este error significa que la API está intentando conectar a MySQL usando un
nombre de contenedor que no existe dentro de la red del contenedor
`stockcito-api`.

En el log puede verse algo como:

```text
Caused by: java.net.UnknownHostException: db-stockcito-db-1
No se pudo conectar a MySQL con host=db-stockcito-db-1
```

Para producción en AWS, corregir `.env.prod` para apuntar a RDS:

```bash
nano .env.prod
```

Debe quedar así:

```text
DB_HOST=<endpoint-rds>
DB_PORT=3306
DB_NAME=stockcito_db
DB_USER=stockcito
DB_PASSWORD=<password-rds>
```

Aplicar los cambios recreando el contenedor:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build --force-recreate
docker logs stockcito-api --tail 80
```

Validar conectividad desde la EC2:

```bash
mysql -h <endpoint-rds> -u stockcito -p stockcito_db
```

Si ese comando no conecta, revisar:

- Que el endpoint de RDS esté bien escrito.
- Que RDS esté en la misma VPC o sea alcanzable desde la EC2.
- Que el security group de RDS permita `3306` desde el security group de la EC2.
- Que usuario, password y base coincidan con `.env.prod`.

Si temporalmente se quiere probar con MySQL en Docker en lugar de RDS, la API y
MySQL deben estar en la misma red Docker. En ese caso usar `DB_HOST=stockcito-db`
y conectar el contenedor de la API a la red de la base:

```bash
docker network ls
docker network connect db_stockcito-net stockcito-api
```

El nombre exacto de la red puede variar según la carpeta desde donde se levantó
`db/docker-compose.yml`.
