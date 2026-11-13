# Backend â€” Sistema de GestiÃ³n de Pedidos de Restaurante

API REST del backend de un sistema integral para la gestiÃ³n operativa y
administrativa de un restaurante: productos, mesas, pedidos, comandas por
Ã¡rea, impresiÃ³n (patrÃ³n outbox), usuarios con permisos por rol y, en fases
posteriores, cuenta, anulaciones, cobro, facturaciÃ³n, caja y reportes.

## Stack

- **Java 25 (LTS)** + **Spring Boot 4.1.x**
- **Spring Modulith 2.x**: monolito modular, fronteras verificadas en pruebas
- **PostgreSQL 16** + **Flyway** (migraciones versionadas)
- **Spring Security + JWT** (HS256, permisos por rol y mÃ©todo)
- **springdoc-openapi** (Swagger UI `/swagger-ui.html`)
- Errores con **ProblemDetail (RFC 7807)**
- **JUnit 5 + Testcontainers** (pruebas contra PostgreSQL real)
- Dinero siempre con **`BigDecimal`** (`Money`, redondeo definido en un solo lugar)

## MÃ³dulos (Spring Modulith)

Cada mÃ³dulo vive en `com.restaurante.<modulo>` con los sub-paquetes
`domain`, `application`, `web` e `infrastructure`:

`usuarios`, `catalogo`, `mesas`, `pedidos`, `comandas`, `configuracion`,
`shared` (base de dominio) y, en fases siguientes: `anulaciones`, `cuentas`,
`pagos`, `facturacion`, `caja`, `finanzas`, `reportes`, `auditoria`,
`clientes`.

## Requisitos

- JDK 25 (Temurin) con `JAVA_HOME` apuntando a Ã©l
- Docker (ejecuciÃ³n del backend y pruebas con Testcontainers); una instancia
  PostgreSQL externa accesible

## Puesta en marcha (local)

Tienes dos alternativas para ejecutar el backend:

### OpciÃ³n 1: Con Docker Compose

Desde la raÃ­z del repositorio:

```bash
docker compose up --build -d
```

Compose inicia solo el backend. Configura `DB_HOST`, `DB_PORT`, `DB_NAME`,
`DB_USER`, `DB_PASSWORD` y `DB_SSLMODE` en `backend/.env` o en el `.env` de la
raÃ­z (este Ãºltimo prevalece si repites variables). La base debe existir y ser
accesible desde Docker. Flyway aplica las migraciones al iniciar, por lo que el
usuario necesita permisos para crear y modificar los objetos del esquema. Si
la BD estÃ¡ en el mismo equipo que Docker, usa `DB_HOST=host.docker.internal`;
para un host remoto, usa su DNS o IP. TLS es obligatorio por defecto
(`DB_SSLMODE=require`); usa `disable` solo si el servidor no admite TLS.

### OpciÃ³n 2: De forma nativa con Maven

Desde la raÃ­z del repositorio utilizando el script utilitario (que carga automÃ¡ticamente `.env` y el JDK):

```bash
./scripts/run-backend.sh
```

O directamente dentro del directorio `backend/`:

```bash
cd backend
./mvnw -q clean package -DskipTests
java -jar target/*.jar        # o: ./mvnw spring-boot:run
```

La aplicaciÃ³n queda en `http://localhost:8080` (health real:
`/actuator/health`). En desarrollo se probÃ³ en otros puertos con
`--server.port=8081`.

### ConfiguraciÃ³n por variables de entorno

La aplicaciÃ³n arma la URL JDBC con `DB_HOST`, `DB_PORT` y `DB_NAME`, y lee el
archivo `.env` **por sÃ­ sola**: `spring.config.import` busca `./.env` y
`./backend/.env`, asÃ­ que funciona tanto arrancando desde `backend/` como desde
la raÃ­z del repo. Para Develop se usa `./scripts/run-backend.sh`, que ademÃ¡s
exporta las variables al entorno.

Ojo con el perfil: `SPRING_PROFILES_ACTIVE` **no activa el perfil si estÃ¡ dentro
de `.env`** (verificado: el perfil se decide antes de procesar los archivos
importados). Debe llegar como variable real del entorno o como propiedad de
sistema:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run    # dentro de backend/
```

Sin perfil la app arranca, pero con la configuraciÃ³n **fail-closed**: sin
Swagger, sin `/actuator/metrics`, sin CORS y con `/actuator/health` sin
detalles. Eso es intencionado, no un error de arranque.

El archivo `.env` local estÃ¡ ignorado por Git. No subas credenciales al
repositorio. Genera el secreto del JWT con `openssl rand -hex 64` y ponlo en
`JWT_SECRET`: no hay valor por defecto en ningÃºn perfil, sin el secreto la
aplicaciÃ³n se niega a arrancar en vez de firmar tokens con una clave conocida.

Perfiles: no hay perfil por defecto (a propÃ³sito). `dev` es el Ãºnico que abre
Swagger, mÃ©tricas y CORS con comodÃ­n; `prod` exige `JWT_SECRET`,
`ADMIN_INITIAL_PASSWORD`, `CORS_ALLOWED_ORIGINS` (sin `*`) y las credenciales de
base de datos.

| Variable | Default | DescripciÃ³n |
| --- | --- | --- |
| `DB_HOST` | `localhost` | Host PostgreSQL (en Docker, configura el host externo; si corre en el equipo local, `host.docker.internal`) |
| `DB_PORT` | `5432` | Puerto PostgreSQL |
| `DB_NAME` | `restaurante` | Base de datos |
| `DB_USER` | `restaurante` | Usuario de BD |
| `DB_PASSWORD` | *(obligatoria en el entorno)* | ContraseÃ±a de BD |
| `DB_SSLMODE` | `require` | Modo TLS de PostgreSQL; `disable` para servidores sin TLS |
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `JWT_SECRET` | *(obligatorio en todos los perfiles)* | Secreto de firma HS512, mÃ­nimo 32 chars (64+ recomendado). Sin Ã©l la app no arranca. |
| `JWT_EXPIRATION_MS` | `3600000` | ExpiraciÃ³n del access token (1 hora) |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | ExpiraciÃ³n del refresh token |
| `IVA_RATE` | `0.15` | Tasa de IVA (15 %, incluido en precios) |
| `ADMIN_INITIAL_PASSWORD` | `admin123` *(solo dev/test)* | Password inicial del usuario `admin`; obligatoria en prod |
| `ADMIN_INITIAL_EMAIL` | `admin` *(solo dev)* | Correo usado como nombre de acceso del administrador inicial; obligatorio en prod |
| `BREVO_HABILITADO` | `false` | Enciende el envÃ­o real por Brevo. Con `false` el correo se escribe en el log (dev/test). |
| `BREVO_API_KEY` | *(obligatoria con Brevo activo)* | Clave de la API transaccional de Brevo |
| `BREVO_REMITENTE_EMAIL` | *(obligatoria con Brevo activo)* | Remitente verificado en la cuenta de Brevo |
| `BREVO_REMITENTE_NOMBRE` | `EstaciÃ³n Burger` | Nombre visible del remitente |
| `BREVO_TIMEOUT` | `5s` | Timeout de conexiÃ³n y de la peticiÃ³n |
| `SPRING_PROFILES_ACTIVE` | *(sin default)* | Perfil activo (`dev` o `prod`). La app no fija perfil por defecto: sin esta variable arranca con la configuraciÃ³n base. |

### Correo transaccional (Brevo)

La verificaciÃ³n de correo y la recuperaciÃ³n de contraseÃ±a envÃ­an el cÃ³digo por
la API de Brevo (`POST /v3/smtp/email`). El contenido sale de las plantillas
versionadas en `src/main/resources/correo/` (`verificacion|recuperacion`
`.html` y `.txt`), no de plantillas del panel de Brevo.

- En desarrollo y pruebas `BREVO_HABILITADO=false`: el cÃ³digo se registra en el
  log del backend para entrega manual (`EmisorCorreoLog`).
- En `prod` el perfil fuerza `habilitado=true` y la app **no arranca** si falta
  `BREVO_API_KEY` o `BREVO_REMITENTE_EMAIL`.
- El envÃ­o ocurre **despuÃ©s** de confirmar la transacciÃ³n: si el registro se
  revierte no sale correo, y si Brevo falla la cuenta ya estÃ¡ creada (se
  registra el fallo sin cÃ³digo ni token) y el usuario puede pedir el reenvÃ­o.
- Brevo responde `400` si el remitente no estÃ¡ verificado en la cuenta.

### Usuario inicial

En desarrollo y tests el usuario `admin` usa la contraseÃ±a `admin123`.
En producciÃ³n no se crea a menos que exista la variable
`ADMIN_INITIAL_PASSWORD`.

## Pruebas

```bash
cd backend
./mvnw test          # requiere Docker (Testcontainers + PostgreSQL real)
./mvnw test -Dtest=ComandaTest   # un test concreto (unit, sin Docker)
```

## API

- Base URL: `/api/v1`
- AutenticaciÃ³n: `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`
- Docs interactivas: `/swagger-ui.html`
- MenÃº pÃºblico (QR, sin autenticaciÃ³n): `GET /api/v1/menu`

## Docker

Imagen multi-stage (build con Maven Wrapper + runtime temurin JRE):

```bash
cd backend
docker build -t restaurant-backend .
docker run --rm -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5432 \
  -e DB_NAME=restaurante \
  -e DB_USER=restaurante \
  -e DB_PASSWORD="$DB_PASSWORD" \
  -e JWT_SECRET="<secreto-512-bits>" \
  restaurant-backend
```

## Despliegue producciÃ³n (Fase 8)

```bash
# Requiere JWT_SECRET â‰¥32 chars y ADMIN_INITIAL_PASSWORD en prod
export JWT_SECRET="cambia-esto-por-un-secreto-512-bits-min-32"
export ADMIN_INITIAL_PASSWORD="cambia-admin-prod"
export DB_HOST="192.168.1.230"
export DB_PORT="5432"
export DB_NAME="manage_restauran"
export DB_USER="elpajarowtf"
export DB_PASSWORD="cambia-db-prod"
docker compose -f ../docker-compose.prod.yml up --build -d
curl http://localhost:8080/actuator/health  # {"status":"UP"}
curl http://localhost:8080/v3/api-docs | jq .info.title
open http://localhost:8080/swagger-ui.html
```

`docker-compose.prod.yml` levanta la app (build `backend/Dockerfile` multi-stage JDK25â†’JRE, usuario no root, healthcheck `/actuator/health`, Flyway migrate al arrancar, `SPRING_PROFILES_ACTIVE=prod`) y conecta a PostgreSQL externo configurado por `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` y `DB_PASSWORD`.

`application-prod.yml` arma la URL JDBC desde las variables de conexiÃ³n y usa `JWT_SECRET`/`ADMIN_INITIAL_PASSWORD` vÃ­a entorno, pool Hikari 20/5, `validate` y `health.probes.enabled`.

## Respaldos (RNF-05)

```bash
./database/backup.sh                          # ./database/backups/restaurante_YYYYMMDD_HHMMSS.dump
./database/backup.sh /tmp/manual.dump
./database/restore.sh ./database/backups/restaurante_20260922_120000.dump  # DROP+CREATE+pg_restore --if-exists
```

El backup usa `pg_dump -F c` (custom) contra el servidor PostgreSQL configurado; requiere `pg_dump` en el host o un cliente PostgreSQL disponible. ValidaciÃ³n con `pg_restore --list`.

## Pruebas de carga (RNF-04/RNF-17)

`CargaConcurrenteTest` â€” 50 hilos `POST /clientes/pedidos/{codigo}/confirmar` con misma `Idempotency-Key`, verifica 0 duplicadas, `@Version` y `UNIQUE(cliente_id,idempotency_key)`:

```bash
./backend/mvnw -f backend/pom.xml test -Dtest=CargaConcurrenteTest
```

Ver `docs/operacion/pruebas-carga.md` y `docs/api/README.md`. Rate limit login `5/min` por IP (429 `urn:problem:restaurante:rate-limit`, deshabilitado en `test`).

## CI

GitHub Actions (`../.github/workflows/ci.yml`) compila y ejecuta toda la
suite (build â†’ test con Testcontainers) desde esta carpeta.
