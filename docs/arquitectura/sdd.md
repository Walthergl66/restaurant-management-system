# SDD — Diseño del sistema

## Contexto

Monorepo con tres clientes (API/backend, web y móvil) y PostgreSQL externo. La web llama a la API mediante rutas `/api/*` proxificadas por Next.js. El backend persiste en PostgreSQL y ejecuta Flyway al arrancar.

## Backend

Aplicación Spring Boot modular. Los módulos siguen paquetes `domain`, `application`, `web` e `infrastructure` donde aplica; los controladores HTTP traducen solicitudes a casos de uso y persistencia PostgreSQL se gestiona con Spring Data/JPA. Seguridad JWT y permisos se implementan en usuarios/seguridad. Esquemas y migraciones están bajo recursos del backend.

## Clientes

Web Next.js App Router organiza pantallas por features y comparte cliente HTTP/estado. Móvil usa Expo Router y configuración central de URL API. Ver README de cada componente.

## Persistencia y contratos

PostgreSQL organiza datos por schemas funcionales; Flyway mantiene migraciones. Contratos REST bajo `/api/v1`; detalles y roles en [API](../api.md). Integraciones de correo transaccional usan Brevo según configuración.

## Decisiones y límites

Compose incluye web y backend, no base de datos ni app móvil. No se identificó un documento formal de ADR completo; consultar `decision-escalado.md` para criterios de escalado. Los diagramas y límites de transacción por caso de uso siguen pendientes de validación con los módulos.
