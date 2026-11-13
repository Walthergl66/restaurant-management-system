# TRD — Requisitos técnicos

## Alcance

Este documento registra las tecnologías y requisitos técnicos observables en el repositorio. Las versiones descritas son las declaradas en archivos de construcción/configuración; confirmar versiones desplegadas en cada entorno.

## Componentes

| Componente | Tecnología | Evidencia |
|---|---|---|
| API | Java 25, Spring Boot 4.1, Maven | `backend/pom.xml`, `backend/Dockerfile` |
| Persistencia | PostgreSQL, Spring Data JPA, Hibernate, Flyway | `backend/pom.xml`, `application.yml` |
| Web | Next.js 16, React 19, TypeScript, Tailwind CSS 4 | `frontend/package.json` |
| Móvil | Expo / React Native | `mobile/package.json` |
| Infraestructura local | Docker Compose; API y web en contenedores, PostgreSQL externo | `docker-compose.yml` |

## Requisitos de ejecución

- PostgreSQL externo accesible por TCP, con base creada y credenciales configuradas mediante `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`.
- TLS PostgreSQL es requerido por defecto (`DB_SSLMODE=require`); ajustar solo conforme a la configuración del servidor.
- Flyway ejecuta migraciones durante el inicio de API. La cuenta necesita permisos para aplicar cambios de esquema.
- API: puerto interno 8080; web: puerto interno 3000 y publicación local por defecto en 7070 mediante Compose. La web envía `/api/*` al backend mediante rewrite de Next.js.
- En Docker, `BACKEND_URL=http://backend:8080`; desde el navegador se usa el mismo origen web.
- Secretos y valores específicos de despliegue se configuran por entorno; no versionar archivos `.env` con credenciales.

## Calidad y verificación

El backend incluye pruebas JUnit, Testcontainers, Spring Modulith y ArchUnit; frontend define scripts de build y ESLint. El pipeline y los comandos de ejecución están descritos en el README raíz. No se fijan aquí niveles de disponibilidad, carga objetivo ni SLA porque no constan como requisitos aprobados.

## Pendientes de decisión

Confirmar proveedor/versiones administradas de PostgreSQL, entornos soportados, objetivos de rendimiento y disponibilidad, política de retención/recuperación, y versiones mínimas de Node/Expo para desarrollo y CI.
