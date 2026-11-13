# Sistema de GestiÃ³n de Restaurante (restaurant-management-system)

Sistema integral para la gestiÃ³n operativa y administrativa de restaurantes: catÃ¡logo de productos, salones y mesas, pedidos presenciales, comandas por Ã¡rea de preparaciÃ³n, cuentas, adiciones, anulaciones, facturaciÃ³n, cierres de caja y auditorÃ­a.

Repositorio estructurado como **monorepo**: cada componente reside en su propio directorio con su propia documentaciÃ³n, configuraciÃ³n por `.env` y ciclo de vida independiente.

---

## ðŸ—ï¸ Estructura del Monorepo

```
.
â”œâ”€â”€ backend/            # API REST (Java 25 LTS + Spring Boot 4.1 + PostgreSQL)
â”‚   â”œâ”€â”€ .env.example    # Plantilla de variables del backend y base de datos
â”‚   â”œâ”€â”€ Dockerfile      # Imagen Docker optimizada (Eclipse Temurin 25 JRE)
â”‚   â”œâ”€â”€ pom.xml         # Dependencias Maven y plugins de compilaciÃ³n
â”‚   â”œâ”€â”€ README.md       # DocumentaciÃ³n tÃ©cnica, endpoints y arquitectura modular
â”‚   â””â”€â”€ src/            # CÃ³digo fuente y migraciones Flyway
â”œâ”€â”€ frontend/           # AplicaciÃ³n Web (Next.js 16 + React 19 + TypeScript + Tailwind)
â”‚   â”œâ”€â”€ .env.example    # Plantilla de variables de Next.js (BACKEND_URL)
â”‚   â”œâ”€â”€ package.json    # Dependencias npm y scripts de ejecuciÃ³n
â”‚   â”œâ”€â”€ README.md       # GuÃ­a de interfaz web, rutas y componentes
â”‚   â””â”€â”€ src/            # Vistas (mesas, pedidos, cocina KDS, caja, auditorÃ­a)
â”œâ”€â”€ mobile/             # App mÃ³vil (Expo 57 + React Native + Expo Router)
â”‚   â”œâ”€â”€ app.json        # ConfiguraciÃ³n de Expo (bundle, splash, apiUrl)
â”‚   â”œâ”€â”€ package.json    # Dependencias npm y scripts (start, android, ios, web)
â”‚   â”œâ”€â”€ README.md       # GuÃ­a de la app mÃ³vil (cliente y mesero)
â”‚   â””â”€â”€ src/            # Rutas Expo Router, features/ y shared/
â”œâ”€â”€ database/           # Herramientas y utilidades de base de datos
â”‚   â”œâ”€â”€ backups/        # Directorio local para almacenamiento de respaldos (.dump)
â”‚   â”œâ”€â”€ schema/         # Esquema consolidado en SQL (material de la prÃ¡ctica)
â”‚   â”œâ”€â”€ backup.sh       # Script para generar copias de seguridad (lee backend/.env)
â”‚   â”œâ”€â”€ restore.sh      # Script para restaurar copias de seguridad (lee backend/.env)
â”‚   â””â”€â”€ README.md       # DocumentaciÃ³n de motor, esquema y respaldo
â”œâ”€â”€ docs/               # DocumentaciÃ³n complementaria (API, carga, producciÃ³n, escalado)
â”œâ”€â”€ docker-compose.yml  # ConfiguraciÃ³n para ejecutar el Backend en Docker (lee backend/.env)
â””â”€â”€ README.md           # GuÃ­a principal del repositorio
```

---

## âš¡ Puesta en Marcha

### 1. Variables de Entorno (Por MÃ³dulo)

Cada aplicaciÃ³n gestiona sus variables de manera independiente:

#### Backend
Copia la plantilla y configura la conexiÃ³n a PostgreSQL:
```bash
cp backend/.env.example backend/.env
```

#### Frontend
Copia la plantilla para la app web:
```bash
cp frontend/.env.example frontend/.env
```

#### App móvil
Copia `mobile/.env.example` a `mobile/.env`. La detección de la IP de Expo permite desarrollo local sin configurar `EXPO_PUBLIC_API_URL`; configura esa variable para builds standalone y añade `EXPO_PUBLIC_MAPBOX_TOKEN` si se usan mapas.

---

### 2. Backend

Tienes dos opciones para ejecutar el backend:

#### OpciÃ³n A: Con Docker (Recomendado)

`docker compose` levanta el backend y la aplicaciÃ³n web. Configura primero la conexiÃ³n a la
instancia PostgreSQL externa en `backend/.env`:

```bash
docker compose --env-file backend/.env --env-file frontend/.env up --build -d
```

- API: `http://localhost:8080`.
- Web: `http://localhost:7070` (puerto configurable con `FRONTEND_PORT`).
- Healthcheck: `http://localhost:8080/actuator/health`
- DocumentaciÃ³n OpenAPI/Swagger: `http://localhost:8080/swagger-ui.html`

Para detener el backend:

```bash
docker compose down
```

La base de datos debe existir y ser accesible desde el contenedor. Flyway aplica
las migraciones al iniciar el backend; el usuario configurado debe tener los
permisos necesarios sobre esa base. Si PostgreSQL estÃ¡ en el mismo equipo que
Docker, usa `DB_HOST=host.docker.internal`; para otro servidor, indica su host o
IP. La conexiÃ³n usa TLS (`DB_SSLMODE=require`) por defecto; configura
`DB_SSLMODE=disable` solo si el servidor no ofrece TLS.

#### OpciÃ³n B: De forma nativa con Maven

Requiere **JDK 25** (Temurin) y Maven 3.9+ (el repo incluye Maven Wrapper):

```bash
cd backend
./mvnw spring-boot:run
```

- La API estarÃ¡ disponible en `http://localhost:8080`.
- Healthcheck: `http://localhost:8080/actuator/health`
- DocumentaciÃ³n OpenAPI/Swagger: `http://localhost:8080/swagger-ui.html`

> Variables de entorno: revisa `backend/.env.example`. Las credenciales de la base
> de datos y `JWT_SECRET` **no** se versionan.

---

### 3. Frontend

El frontend se ejecuta directamente en tu mÃ¡quina con Node.js (22):

```bash
cd frontend
npm install
npm run dev
```

El frontend estarÃ¡ disponible en `http://localhost:3000` y redirige automÃ¡ticamente las llamadas de API a `http://localhost:8080`.

- Build de producciÃ³n: `npm run build`
- Lint: `npm run lint`

---

### 4. App mÃ³vil (opcional)

La app mÃ³vil (Expo SDK 57) cubre dos perfiles: **cliente** (menÃº, carrito, checkout,
pedidos en vivo, direcciones, mÃ©todos de pago) y **mesero** (atenciÃ³n de mesas).

```bash
cd mobile
npm install
npx expo start
```

- Android: `npx expo start --android`
- iOS: `npx expo start --ios`
- Web (Expo): `npx expo start --web`

**ConexiÃ³n con la API:** la URL base se resuelve en este orden
(`mobile/src/config/environment.ts`):

1. `EXPO_PUBLIC_API_URL` en `mobile/.env` (variable de entorno al bundlear).
2. En desarrollo, la IP del PC deducida del host del dev server de Expo.
3. `extra.apiUrl` de `app.json` (builds sin dev server).
4. Default: `http://localhost:8080/api/v1`.

> En un telÃ©fono fÃ­sico `localhost` apunta al propio telÃ©fono: por eso el punto 2
> deriva la IP de la mÃ¡quina de desarrollo. Para forzar otra URL:
> Copia `mobile/.env.example` a `mobile/.env` y define `EXPO_PUBLIC_API_URL=http://192.168.1.50:8080/api/v1`.

---

### 5. Base de Datos (PostgreSQL)

- **Migraciones:** Se aplican automÃ¡ticamente al iniciar el backend mediante Flyway.
- **Esquema:** 7 schemas por responsabilidad (`seguridad`, `catalogo`, `configuracion`,
  `operaciones`, `comercial`, `tesoreria`, `auditoria`) y 35 tablas. El SQL consolidado
  vive en `database/schema/restaurante_completo.sql`.
- **Respaldos:**
  ```bash
  ./database/backup.sh
  ```
- **RestauraciÃ³n:**
  ```bash
  ./database/restore.sh ./database/backups/<archivo.dump>
  ```

---

## ðŸ§ª Pruebas y CI

```bash
cd backend
./mvnw test          # 215 tests (JUnit 5 + Testcontainers con PostgreSQL real)
```

Las pruebas requieren Docker levantado (Testcontainers levanta un PostgreSQL efÃ­mero).
El build incluye `ModularityTests` (fronteras entre mÃ³dulos con Spring Modulith) y
`ArquitecturaTests` (reglas de capas con ArchUnit).

**CI** (`.github/workflows/ci.yml`): en cada push a `main` y en cada PR se compila y
prueban el backend (JDK 25) y se lintea y compila el frontend (Node 22).

---

## ðŸš€ Despliegue en ProducciÃ³n

Para entornos productivos, utiliza `docker-compose.prod.yml` que valida obligatoriamente variables seguras (`JWT_SECRET`, `ADMIN_INITIAL_EMAIL`, `ADMIN_INITIAL_PASSWORD`, `CORS_ALLOWED_ORIGINS` sin comodines):

```bash
docker compose --env-file backend/.env -f docker-compose.prod.yml up --build -d
```

Consulta mÃ¡s detalles en [docs/operacion/despliegue.md](docs/operacion/despliegue.md).

DocumentaciÃ³n complementaria:

- [docs/api/README.md](docs/api/README.md) â€” endpoints, permisos por rol, errores y WebSocket.
- [docs/operacion/pruebas-carga.md](docs/operacion/pruebas-carga.md) â€” pruebas de carga.
- [docs/arquitectura/decisiones/escalado.md](docs/arquitectura/decisiones/escalado.md) â€” criterios de escalado.
- [backend/README.md](backend/README.md) â€” detalle tÃ©cnico del API y arquitectura modular.
- [frontend/README.md](frontend/README.md) â€” rutas y componentes de la web.
- [mobile/README.md](mobile/README.md) â€” estructura de la app mÃ³vil.

## Configuración de entorno

Usa `backend/.env.example`, `frontend/.env.example` y `mobile/.env.example` como plantillas de cada componente. No hay archivos de entorno compartidos en la raíz. Para Compose carga explícitamente `backend/.env` y `frontend/.env` con `--env-file`; para producción basta `backend/.env`.
