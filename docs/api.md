# API — restaurant-management-system

Base URL: `http://localhost:8080/api/v1` (prod) o `http://localhost:8080` (dev, `SERVER_PORT`).

## Autenticación

- `POST /api/v1/auth/login` → `{accessToken, refreshToken, usuario}` (JWT HS256 32+ chars, 24h)
- `POST /api/v1/auth/refresh` → renueva `accessToken` con `refreshToken`
- `POST /api/v1/auth/logout` → revoca `refreshToken`
- `GET /api/v1/auth/me` → usuario actual
- `POST /api/v1/auth/registro` → alta de cliente (correo, nombre, cédula, celular, contraseña) → 201, pendiente de verificar el correo
- `POST /api/v1/auth/registro-repartidor` → alta de repartidor (correo, nombre, celular, contraseña) → 201, pendiente de verificar el correo
- `POST /api/v1/auth/verificar-email` → `{username, codigo}` → tokens si el código es válido
- `POST /api/v1/auth/reenviar-verificacion` → `{username}` → 204 siempre (no revela si la cuenta existe)

Header: `Authorization: Bearer <accessToken>`.

Rate limit: `POST /login`, `/refresh`, `/registro` y `/reenviar-verificacion`
→ 5/min por IP (429 `urn:problem:restaurante:rate-limit`), deshabilitado en
`test`. El de registro/reenvío protege la cuota del proveedor de correo.

## Módulos

| Módulo | Prefijo | Permisos |
|---|---|---|
| usuarios | `/api/v1/usuarios`, `/api/v1/auth` | `usuarios:ver/crear/editar/eliminar`, `roles:asignar` |
| catalogo | `/api/v1/categorias`, `/api/v1/areas`, `/api/v1/extras`, `/api/v1/productos` | `catalogo:ver/gestionar` |
| mesas | `/api/v1/mesas` | `mesas:ver/gestionar` |
| pedidos | `/api/v1/pedidos` | `pedidos:ver/crear/editar/confirmar`, `pedidos:estado-preparacion/listo`, `pedidos:entregar`, `pedidos:retiro-mostrador` |
| comandas | `/api/v1/comandas`, `/api/v1/comandas/impresion/pendientes` | `comandas:ver/reenviar` |
| clientes | `/api/v1/clientes` (carrito, pedidos, historial, direcciones) | `clientes:menu-ver`, `clientes:carrito-gestionar`, `clientes:pedido-crear/confirmar`, `clientes:historial-ver`, `clientes:cuenta-nueva` |
| cuentas | `/api/v1/cuentas` | `cuentas:ver/gestionar` |
| pagos/facturación/caja | `/api/v1/cuentas/{id}/cobros`, `/api/v1/cajas` | `pagos:cobrar`, `facturacion:emitir`, `caja:apertura/movimientos/cierre` |
| finanzas/reportes/auditoria | `/api/v1/finanzas`, `/api/v1/reportes`, `/api/v1/auditoria` | `finanzas:ver`, `reportes:ver`, `auditoria:ver` |
| menú público | `GET /api/v1/menu` (sin auth, QR) | — |
| salud | `GET /actuator/health` | — |
| docs | `GET /v3/api-docs`, `GET /swagger-ui.html` | — |

## Roles

Los roles viven en categorías y lo que el panel puede hacer con cada uno son
**banderas de la base de datos** (`roles.asignable_por_admin`, `roles.configurable`,
`roles.origen`), no listas en el código.

| Categoría | Subroles | Asignable | Configurable | Origen |
|---|---|---|---|---|
| `ADMIN` | — | no | no | `ADMIN` |
| `EMPLEADO` | `MESERO`, `CAJERO` | sí | sí | `ADMIN` |
| `CLIENTE` | — | no | no | `AUTO_REGISTRO` |
| `DELIVERY` | — | no | no | `AUTO_REGISTRO` |

- `GET /api/v1/usuarios/roles` → taxonomía: `{categorias:[{codigo,descripcion,roles[]}], roles[]}`.
  Cada rol trae `categoria`, `asignablePorAdmin`, `configurable`, `origen` y sus `permisos` por defecto.
- `GET /api/v1/usuarios/permisos` → catálogo de funcionalidades para el checklist del panel.
- El administrador asigna funcionalidades por usuario solo en los roles **configurables**
  (`MESERO`, `CAJERO`); si el usuario no tiene permisos explícitos, rigen los de su rol.
- Crear o cambiar a un rol **no asignable** se rechaza con **422**
  (`urn:problem:restaurante:regla-de-negocio`); un rol no configurable con permisos
  explícitos, también.
- `ADMIN` es una cuenta única (la que se siembra); `CLIENTE` y `DELIVERY` nacen por
  auto-registro desde la app.

## Versionado y errores

- Versionado en ruta `/api/v1`.
- Errores `ProblemDetail` RFC 7807 (`type: urn:problem:restaurante:*`, `title`, `detail`, `path`, `fieldErrors`).
- Códigos: `401` no autenticado, `403` acceso denegado, `409` conflicto (código duplicado, idempotencia distinta, `DataIntegrityViolation`/`@Version`), `422` regla de negocio.

## Dinero

Siempre `Money` (`com.restaurante.shared.domain.Money:23`) — `NUMERIC(12,2)`, `SCALE 2 HALF_UP`, nunca `double`.

## WebSocket (RF-43)

`@SubscribeMapping("/topic/pedido/{codigo}")` → `PedidoClienteSPI` en vivo (STOMP, agente local imprime vía `outbox`).

## OpenAPI

`springdoc-openapi` 3.1.1, `spring-boot-starter-webmvc` + `spring-boot-starter-jackson` (Jackson 3 `tools.jackson`). Ver `backend/src/main/resources/application.yml:23` (`springdoc.swagger-ui.path`).

```bash
curl http://localhost:8080/v3/api-docs | jq .info.title
open http://localhost:8080/swagger-ui.html
```
