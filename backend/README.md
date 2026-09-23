# Backend — Sistema de Gestión de Pedidos de Restaurante

API REST del backend de un sistema integral para la gestión operativa y
administrativa de un restaurante: productos, mesas, pedidos, comandas por
área, impresión (patrón outbox), usuarios con permisos por rol y, en fases
posteriores, cuenta, anulaciones, cobro, facturación, caja y reportes.

## Stack

- **Java 25 (LTS)** + **Spring Boot 4.1.x**
- **Spring Modulith 2.x**: monolito modular, fronteras verificadas en pruebas
- **PostgreSQL 16** + **Flyway** (migraciones versionadas)
- **Spring Security + JWT** (HS512, permisos por rol y método)
- **springdoc-openapi** (Swagger UI `/swagger-ui.html`)
- Errores con **ProblemDetail (RFC 7807)**
- **JUnit 5 + Testcontainers** (pruebas contra PostgreSQL real)
- Dinero siempre con **`BigDecimal`** (`Money`, redondeo definido en un solo lugar)

## Módulos (Spring Modulith)

Cada módulo vive en `com.restaurante.<modulo>` con los sub-paquetes
`domain`, `application`, `web` e `infrastructure`:

`usuarios`, `catalogo`, `mesas`, `pedidos`, `comandas`, `configuracion`,
`shared` (base de dominio) y, en fases siguientes: `anulaciones`, `cuentas`,
`pagos`, `facturacion`, `caja`, `finanzas`, `reportes`, `auditoria`,
`clientes`.

## Requisitos

- JDK 25 (Temurin) con `JAVA_HOME` apuntando a él
- Docker (base de datos local y pruebas con Testcontainers)

## Puesta en marcha (local)

Desde la raíz del repo (donde está el `docker-compose.yml` con PostgreSQL):

```bash
docker compose up -d postgres
cd backend
./mvnw -q clean package -DskipTests
java -jar target/*.jar        # o: ./mvnw spring-boot:run
```

La aplicación queda en `http://localhost:8080` (health real:
`/actuator/health`). En desarrollo se probó en otros puertos con
`--server.port=8081`.

### Configuración por variables de entorno

| Variable | Default | Descripción |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/restaurante` | URL de la base |
| `DB_USERNAME` | `restaurante` | Usuario de BD |
| `DB_PASSWORD` | `restaurante` | Contraseña de BD |
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `JWT_SECRET` | *(vacío)* | Secreto HS512 (obligatorio en prod) |
| `JWT_EXPIRATION_MS` | `86400000` | Expiración del access token |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | Expiración del refresh token |
| `IVA_RATE` | `0.15` | Tasa de IVA (15 %, incluido en precios) |
| `ADMIN_INITIAL_PASSWORD` | *(solo prod)* | Password inicial del usuario `admin` |

### Usuario inicial

En desarrollo y tests el usuario `admin` usa la contraseña `admin123`.
En producción no se crea a menos que exista la variable
`ADMIN_INITIAL_PASSWORD`.

## Pruebas

```bash
cd backend
./mvnw test          # requiere Docker (Testcontainers + PostgreSQL real)
./mvnw test -Dtest=ComandaTest   # un test concreto (unit, sin Docker)
```

## API

- Base URL: `/api/v1`
- Autenticación: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`
- Docs interactivas: `/swagger-ui.html`
- Menú público (QR, sin autenticación): `GET /api/v1/menu`

## Docker

Imagen multi-stage (build con Maven Wrapper + runtime temurin JRE):

```bash
cd backend
docker build -t restaurant-backend .
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/restaurante \
  -e JWT_SECRET="<secreto-512-bits>" \
  restaurant-backend
```

## Despliegue producción (Fase 8)

```bash
# Requiere JWT_SECRET ≥32 chars y ADMIN_INITIAL_PASSWORD en prod
export JWT_SECRET="cambia-esto-por-un-secreto-512-bits-min-32"
export ADMIN_INITIAL_PASSWORD="cambia-admin-prod"
export DB_PASSWORD="cambia-db-prod"
docker compose -f ../docker-compose.prod.yml up --build -d
curl http://localhost:8080/actuator/health  # {"status":"UP"}
curl http://localhost:8080/v3/api-docs | jq .info.title
open http://localhost:8080/swagger-ui.html
```

`docker-compose.prod.yml` levanta `postgres:16` + `app` (build `backend/Dockerfile` multi-stage JDK25→JRE, usuario no root, healthcheck `/actuator/health`, Flyway migrate al arrancar, `SPRING_PROFILES_ACTIVE=prod`).

`application-prod.yml` usa `DB_URL`/`JWT_SECRET`/`ADMIN_INITIAL_PASSWORD` vía env, pool Hikari 20/5, `validate` y `health.probes.enabled`.

## Respaldos (RNF-05)

```bash
./database/backup.sh                          # ./database/backups/restaurante_YYYYMMDD_HHMMSS.dump
./database/backup.sh /tmp/manual.dump
./database/restore.sh ./database/backups/restaurante_20260922_120000.dump  # DROP+CREATE+pg_restore --if-exists
```

El backup usa `pg_dump -F c` (custom) contra el contenedor `restaurante-postgres` o directo. Validación con `pg_restore --list`.

## Pruebas de carga (RNF-04/RNF-17)

`CargaConcurrenteTest` — 50 hilos `POST /clientes/pedidos/{codigo}/confirmar` con misma `Idempotency-Key`, verifica 0 duplicadas, `@Version` y `UNIQUE(cliente_id,idempotency_key)`:

```bash
./backend/mvnw -f backend/pom.xml test -Dtest=CargaConcurrenteTest
```

Ver `docs/carga.md` y `docs/api.md`. Rate limit login `5/min` por IP (429 `urn:problem:restaurante:rate-limit`, deshabilitado en `test`).

## CI

GitHub Actions (`../.github/workflows/ci.yml`) compila y ejecuta toda la
suite (build → test con Testcontainers) desde esta carpeta.