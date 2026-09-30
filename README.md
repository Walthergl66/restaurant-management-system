# Sistema de Gestión de Restaurante (restaurant-management-system)

Sistema integral para la gestión operativa y administrativa de restaurantes: catálogo de productos, salones y mesas, pedidos presenciales, comandas por área de preparación, cuentas, adiciones, anulaciones, facturación, cierres de caja y auditoría.

Repositorio estructurado como **monorepo**: cada componente reside en su propio directorio con su propia documentación, configuración y ciclo de vida independiente.

---

## 🏗️ Estructura del Monorepo

```
.
├── backend/            # API REST (Java 25 LTS + Spring Boot 4.1 + PostgreSQL)
│   ├── Dockerfile      # Imagen Docker optimizada (Eclipse Temurin 25 JRE)
│   ├── pom.xml         # Dependencias Maven y plugins de compilación
│   ├── README.md       # Documentación técnica, endpoints y arquitectura modular
│   └── src/            # Código fuente y migraciones Flyway
├── frontend/           # Aplicación Web (Next.js 16 + React 19 + TypeScript + Tailwind)
│   ├── package.json    # Dependencias npm y scripts de ejecución
│   ├── README.md       # Guía de interfaz web, rutas y componentes
│   └── src/            # Vistas (mesas, pedidos, cocina KDS, caja, auditoría)
├── database/           # Herramientas y utilidades de base de datos
│   ├── backups/        # Directorio local para almacenamiento de respaldos (.dump)
│   ├── backup.sh       # Script para generar copias de seguridad de PostgreSQL
│   ├── restore.sh      # Script para restaurar copias de seguridad
│   └── README.md       # Documentación de motor, esquema y respaldo
├── scripts/            # Scripts utilitarios para ejecución local
│   ├── run-backend.sh  # Inicia el backend cargando .env y JDK local
│   └── run-frontend.sh # Inicia el frontend instalando dependencias si faltan
├── docs/               # Documentación complementaria (API, carga, producción)
├── docker-compose.yml  # Configuración para ejecutar exclusivamente el Backend en Docker
├── .env.example        # Plantilla central de variables de entorno
└── README.md           # Guía principal del repositorio
```

---

## ⚡ Puesta en Marcha

### 1. Variables de Entorno

Copia la plantilla `.env.example` en la raíz como `.env` y ajusta las credenciales de tu PostgreSQL:

```bash
cp .env.example .env
```

---

### 2. Backend

Tienes dos opciones para ejecutar el backend:

#### Opción A: Con Docker (Recomendado para contenedorizar el backend)

Docker está configurado para ejecutar **únicamente** la aplicación backend:

```bash
docker compose up --build -d
```

- La API estará disponible en `http://localhost:8080`.
- Healthcheck: `http://localhost:8080/actuator/health`
- Documentación OpenAPI/Swagger: `http://localhost:8080/swagger-ui.html`

Para detener el contenedor:

```bash
docker compose down
```

#### Opción B: De forma nativa con Maven

```bash
./scripts/run-backend.sh
```

---

### 3. Frontend

El frontend se ejecuta directamente en tu máquina con Node.js:

```bash
# Opción 1: Mediante el script utilitario
./scripts/run-frontend.sh

# Opción 2: Directamente con npm
cd frontend
npm install
npm run dev
```

El frontend estará disponible en `http://localhost:3000` y redirige automáticamente las llamadas de API a `http://localhost:8080`.

---

### 4. Base de Datos (PostgreSQL)

- **Migraciones:** Se aplican automáticamente al iniciar el backend mediante Flyway.
- **Respaldos:**
  ```bash
  ./database/backup.sh
  ```
- **Restauración:**
  ```bash
  ./database/restore.sh ./database/backups/<archivo.dump>
  ```

---

## 🚀 Despliegue en Producción

Para entornos productivos, utiliza `docker-compose.prod.yml` que valida obligatoriamente variables seguras (`JWT_SECRET`, `ADMIN_INITIAL_PASSWORD`, `CORS_ALLOWED_ORIGINS` sin comodines):

```bash
docker compose -f docker-compose.prod.yml up --build -d
```

Consulta más detalles en [docs/produccion.md](file:///home/elpajarowtf/Documentos/octavo/INTEGRACION/restaurant-management-system/docs/produccion.md).