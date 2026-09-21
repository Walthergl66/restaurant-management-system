# restaurant-management-system

Sistema integral para la gestión operativa y administrativa de un restaurante:
productos, mesas, pedidos, comandas por área, impresión, usuarios con permisos,
y en fases posteriores cuenta, anulaciones, cobro, facturación, caja y reportes.

Repositorio tipo **monorepo**: cada aplicación vive en su propia carpeta con su
propio `README.md`, `.gitignore` y `Dockerfile`.

## Estructura

```
.
├── backend/           API REST (Java 25 + Spring Boot 4.1 + PostgreSQL)
│   ├── README.md      Guía del backend
│   ├── Dockerfile     Imagen de producción del backend
│   └── .gitignore     Ignorados propios del backend
├── database-system/   Scripts/herramientas de base de datos
├── docker-compose.yml PostgreSQL local para desarrollo
└── frontend/          (próximamente) App web de clientes
    mobile/            (próximamente) App móvil
```

## Módulos

- **backend/**: API REST, contexto completo en `backend/README.md`
  (stack, variables de entorno, pruebas y Docker).
- **frontend/**: Pendiente.
- **mobile/**: Pendiente.

## Requisitos de desarrollo

- JDK 25 (Temurin) con `JAVA_HOME` configurado
- Docker (PostgreSQL local y pruebas con Testcontainers)

## Puesta en marcha

Base de datos local y luego el backend:

```bash
docker compose up -d postgres
cd backend && ./mvnw -q clean package -DskipTests && java -jar target/*.jar
```

La API queda en `http://localhost:8080` (health: `/actuator/health`,
Swagger: `/swagger-ui.html`).

## CI

GitHub Actions compila y ejecuta las pruebas del backend (`.github/workflows/ci.yml`).