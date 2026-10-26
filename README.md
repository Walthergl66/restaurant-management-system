# Sistema de Gestión de Restaurante (restaurant-management-system)

Almacenamiento local de imágenes y videos: [guía de Ubuntu y configuración](docs/archivos-ubuntu.md).

La guía incluye red de VirtualBox, IP estática, Nginx, permisos, claves SFTP,
preparación de PostgreSQL y uso del selector de archivos. Para actualizar la
configuración local, consulta [qué archivo `.env` editar y qué significa cada variable](docs/variables-entorno.md).

Sistema integral para la gestión operativa y administrativa de restaurantes: catálogo de productos, salones y mesas, pedidos presenciales, comandas por área de preparación, cuentas, adiciones, anulaciones, facturación, cierres de caja y auditoría.

Repositorio estructurado como **monorepo**: cada componente reside en su propio directorio con su propia documentación, configuración por `.env` y ciclo de vida independiente.

---

## 🏗️ Estructura del Monorepo

```
.
├── backend/            # API REST (Java 25 LTS + Spring Boot 4.1 + PostgreSQL)
│   ├── .env.example    # Plantilla de variables para backend y base de datos
│   ├── Dockerfile      # Imagen Docker optimizada (Eclipse Temurin 25 JRE)
│   ├── pom.xml         # Dependencias Maven y plugins de compilación
│   ├── README.md       # Documentación técnica, endpoints y arquitectura modular
│   └── src/            # Código fuente y migraciones Flyway
├── frontend/           # Aplicación Web (Next.js 16 + React 19 + TypeScript + Tailwind)
│   ├── .env.example    # Plantilla de variables de Next.js (BACKEND_URL)
│   ├── package.json    # Dependencias npm y scripts de ejecución
│   ├── README.md       # Guía de interfaz web, rutas y componentes
│   └── src/            # Vistas (mesas, pedidos, cocina KDS, caja, auditoría)
├── mobile/             # App móvil (Expo 57 + React Native + Expo Router)
│   ├── app.json        # Configuración de Expo (bundle, splash, apiUrl)
│   ├── package.json    # Dependencias npm y scripts (start, android, ios, web)
│   ├── README.md       # Guía de la app móvil (cliente y mesero)
│   └── src/            # Rutas Expo Router, features/ y shared/
├── database/           # Herramientas y utilidades de base de datos
│   ├── backups/        # Directorio local para almacenamiento de respaldos (.dump)
│   ├── schema/         # Esquema consolidado en SQL (material de la práctica)
│   ├── backup.sh       # Script para generar copias de seguridad (lee backend/.env)
│   ├── restore.sh      # Script para restaurar copias de seguridad (lee backend/.env)
│   └── README.md       # Documentación de motor, esquema y respaldo
├── docs/               # Documentación complementaria (API, carga, producción, escalado)
├── docker-compose.yml  # Configuración para ejecutar el Backend en Docker (lee backend/.env)
└── README.md           # Guía principal del repositorio
```

---

## ⚡ Puesta en Marcha

### 1. Variables de Entorno (Por Módulo)

Cada aplicación gestiona sus variables de manera independiente:

#### Docker Compose local

Para una instalación nueva, copia también la plantilla de la raíz y adapta sus
valores antes de crear el volumen de PostgreSQL:

```bash
cp .env.example .env
```

Si ya existe `.env`, incorpora las variables nuevas conservando los secretos y
la configuración actual. Compose toma de la raíz los valores que interpola en
su YAML. Los secretos del backend se configuran en `backend/.env`; consulta la
[referencia de variables](docs/variables-entorno.md) para su precedencia.

#### Backend
Copia la plantilla y configura la conexión a PostgreSQL:
```bash
cp backend/.env.example backend/.env
```

#### Frontend
Copia la plantilla para la app web:
```bash
cp frontend/.env.example frontend/.env.local
```

---

### 2. Backend

Tienes dos opciones para ejecutar el backend:

#### Opción A: Con Docker (Recomendado)

`docker compose` levanta **la base de datos PostgreSQL 16 y el backend** (en ese orden,
el backend espera a que la BD esté sana):

```bash
docker compose up --build -d
```

- PostgreSQL: `localhost:5432` (puerto configurable con `DB_HOST_PORT`).
- API: `http://localhost:8080`.
- Healthcheck: `http://localhost:8080/actuator/health`
- Documentación OpenAPI/Swagger: `http://localhost:8080/swagger-ui.html`

Los datos de la BD de desarrollo sobreviven a `docker compose down` (volumen `pgdata`);
para borrarlos también: `docker compose down -v`.

Para detener los contenedores:

```bash
docker compose down
```

#### Opción B: De forma nativa con Maven

Requiere **JDK 25** (Temurin) y Maven 3.9+ (el repo incluye Maven Wrapper):

```bash
cd backend
./mvnw spring-boot:run
```

- La API estará disponible en `http://localhost:8080`.
- Healthcheck: `http://localhost:8080/actuator/health`
- Documentación OpenAPI/Swagger: `http://localhost:8080/swagger-ui.html`

> Variables de entorno: revisa `backend/.env.example`. Las credenciales de la base
> de datos y `JWT_SECRET` **no** se versionan.

---

### 3. Frontend

El frontend se ejecuta directamente en tu máquina con Node.js (22):

```bash
cd frontend
npm install
npm run dev
```

El frontend estará disponible en `http://localhost:3000` y redirige automáticamente las llamadas de API a `http://localhost:8080`.

- Build de producción: `npm run build`
- Lint: `npm run lint`

---

### 4. App móvil (opcional)

La app móvil (Expo SDK 57) cubre dos perfiles: **cliente** (menú, carrito, checkout,
pedidos en vivo, direcciones, métodos de pago) y **mesero** (atención de mesas).

```bash
cd mobile
npm install
npx expo start
```

- Android: `npx expo start --android`
- iOS: `npx expo start --ios`
- Web (Expo): `npx expo start --web`

**Conexión con la API:** la URL base se resuelve en este orden
(`mobile/src/config/environment.ts`):

1. `EXPO_PUBLIC_API_URL` (variable de entorno al bundlear).
2. En desarrollo, la IP del PC deducida del host del dev server de Expo.
3. `extra.apiUrl` de `app.json` (builds sin dev server).
4. Default: `http://localhost:8080/api/v1`.

> En un teléfono físico `localhost` apunta al propio teléfono: por eso el punto 2
> deriva la IP de la máquina de desarrollo. Para forzar otra URL:
> `EXPO_PUBLIC_API_URL=http://192.168.1.50:8080/api/v1 npx expo start`

---

### 5. Base de Datos (PostgreSQL)

- **Migraciones:** Se aplican automáticamente al iniciar el backend mediante Flyway.
- **Esquema vigente:** lo definen las migraciones de Flyway. Incluye los schemas
  `seguridad`, `catalogo`, `configuracion`, `operaciones`, `comercial`, `tesoreria`,
  `auditoria` y `archivos`. `database/schema/restaurante_completo.sql` es material
  de referencia; usa las migraciones del backend para instalar la versión actual.
- **Respaldos:**
  ```bash
  ./database/backup.sh
  ```
- **Restauración:**
  ```bash
  ./database/restore.sh ./database/backups/<archivo.dump>
  ```

---

## 🧪 Pruebas y CI

```bash
cd backend
./mvnw test          # JUnit + Testcontainers con PostgreSQL real
```

Las pruebas requieren Docker levantado (Testcontainers levanta un PostgreSQL efímero).
El build incluye `ModularityTests` (fronteras entre módulos con Spring Modulith) y
`ArquitecturaTests` (reglas de capas con ArchUnit).

**CI** (`.github/workflows/ci.yml`): en cada push a `main` y en cada PR se compila y
prueban el backend (JDK 25) y se lintea y compila el frontend (Node 22).

---

## 🚀 Despliegue en Producción

Para entornos productivos, utiliza `docker-compose.prod.yml` que valida obligatoriamente variables seguras (`JWT_SECRET`, `ADMIN_INITIAL_PASSWORD`, `CORS_ALLOWED_ORIGINS` sin comodines):

```bash
docker compose -f docker-compose.prod.yml up --build -d
```

Consulta más detalles en [docs/produccion.md](docs/produccion.md).

Documentación complementaria:

- [docs/api.md](docs/api.md) — endpoints, permisos por rol, errores y WebSocket.
- [docs/archivos-ubuntu.md](docs/archivos-ubuntu.md) — instalación completa del almacenamiento local y flujo de subida/lectura.
- [docs/variables-entorno.md](docs/variables-entorno.md) — plantillas `.env`, conexión SFTP/HTTP y actualización de configuración.
- [docs/carga.md](docs/carga.md) — pruebas de carga.
- [docs/decision-escalado.md](docs/decision-escalado.md) — criterios de escalado.
- [backend/README.md](backend/README.md) — detalle técnico del API y arquitectura modular.
- [frontend/README.md](frontend/README.md) — rutas y componentes de la web.
- [mobile/README.md](mobile/README.md) — estructura de la app móvil.
