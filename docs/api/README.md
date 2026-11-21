# API — restaurant-management-system

Base URL: `http://localhost:8080/api/v1` (prod) o `http://localhost:8080` (dev, `SERVER_PORT`).

## Autenticación

- El nombre de acceso del administrador inicial es `ADMIN_INITIAL_EMAIL` (correo). En desarrollo, si no se define, conserva el valor `admin`.
- En producción se requieren `ADMIN_INITIAL_EMAIL` y `ADMIN_INITIAL_PASSWORD`. Solo crean la cuenta si todavía no existe ningún usuario con rol `ADMIN`; no modifican una cuenta ya creada.
- `POST /api/v1/auth/login` → `{accessToken, refreshToken, usuario}` (JWT HS256 32+ chars, 24h)
- `POST /api/v1/auth/refresh` → renueva `accessToken` con `refreshToken`
- `POST /api/v1/auth/logout` → revoca `refreshToken`
- `GET /api/v1/auth/me` → usuario actual
- `POST /api/v1/auth/registro` → alta de cliente (correo, nombre, cédula, celular, contraseña) → 201, pendiente de verificar el correo
- `POST /api/v1/auth/registro-repartidor` → alta de repartidor (correo, nombre, celular, contraseña) → 201, pendiente de verificar el correo
- `POST /api/v1/auth/verificar-email` → `{username, codigo}` → tokens si el código es válido

Header: `Authorization: Bearer <accessToken>`.

Rate limit: `POST /login` → 5/min por IP (429 `urn:problem:restaurante:rate-limit`), deshabilitado en `test`.

## Módulos

| Módulo | Prefijo | Permisos |
|---|---|---|
| usuarios | `/api/v1/usuarios`, `/api/v1/auth` | `usuarios:ver/crear/editar/eliminar`, `roles:asignar` |
| catalogo | `/api/v1/categorias`, `/api/v1/areas`, `/api/v1/extras`, `/api/v1/productos` | `catalogo:ver/gestionar` |
| mesas | `/api/v1/mesas`, `/api/v1/mesas/zonas` | `mesas:ver/gestionar` |
| pedidos | `/api/v1/pedidos` | `pedidos:ver/crear/editar/confirmar`, `pedidos:entregar` (`pedidos:estado-preparacion` y `pedidos:estado-listo` se usan en comandas; `pedidos:retiro-mostrador` en el retiro de la app) |
| comandas | `/api/v1/comandas`, `/api/v1/comandas/impresion/pendientes` | `comandas:ver/reenviar`, `pedidos:estado-preparacion`, `pedidos:estado-listo` |
| clientes | `/api/v1/clientes` (carrito, pedidos, historial, direcciones; `/operacion/pedidos` y `/pedidos/{codigo}/entregar` para el mostrador) | `pedidos:retiro-mostrador` (mostrador), `clientes:menu-ver`, `clientes:carrito-gestionar`, `clientes:pedido-crear/confirmar`, `clientes:historial-ver`, `clientes:cuenta-nueva` |
| cuentas | `/api/v1/cuentas` | `cuentas:ver/gestionar` |
| pagos/facturación/caja | `/api/v1/cuentas/{id}/cobros`, `/api/v1/cajas` | `pagos:cobrar`, `facturacion:emitir`, `caja:apertura/movimientos/cierre` |
| finanzas/reportes/auditoria | `/api/v1/finanzas`, `/api/v1/reportes`, `/api/v1/auditoria` | `finanzas:ver`, `reportes:ver`, `auditoria:ver` |
| menú público | `GET /api/v1/menu` (sin auth, QR) | — |
| salud | `GET /actuator/health` | — |
| docs | `GET /v3/api-docs`, `GET /swagger-ui.html` | — |

### Mesas y zonas

Las mesas se ubican en **zonas** del restaurante (Salón, Terraza, Patio…) que define el
administrador. "Zona" no es lo mismo que "área": las áreas del catálogo (`/api/v1/areas`)
son áreas de **preparación** (Cocina, Barra) y dividen las comandas.

| Operación | Endpoint | Permiso |
|---|---|---|
| Listar zonas en su orden, con el número de mesas de cada una | `GET /api/v1/mesas/zonas` | `mesas:ver` |
| Crear una zona (queda al final del orden) | `POST /api/v1/mesas/zonas` `{nombre}` → 201 | `mesas:gestionar` |
| Renombrar | `PUT /api/v1/mesas/zonas/{id}` `{nombre}` | `mesas:gestionar` |
| Guardar el orden de todas las zonas | `PUT /api/v1/mesas/zonas/orden` `{ids:[…]}` | `mesas:gestionar` |
| Eliminar | `DELETE /api/v1/mesas/zonas/{id}` → 204 | `mesas:gestionar` |

Reglas:

- El nombre es obligatorio, de hasta 60 caracteres, y único sin distinguir mayúsculas, espacios ni
  tildes ("Terraza", "terraza " y "TERRAZA" son la misma): un duplicado responde **409**. La primera
  letra se guarda en mayúscula.
- `PUT …/orden` debe recibir **todas** las zonas, cada una una sola vez; si la lista está incompleta
  o repetida responde **422** y no cambia nada.
- Una zona **solo se elimina si no tiene mesas**, y siempre debe quedar al menos una: en otro caso,
  **422**.

Mesas (`/api/v1/mesas`):

- El código visual `M01` se deriva de `numero: 1`; `id` identifica el registro y no es el
  número visible. El código no se envía ni se guarda como campo adicional. El formulario web
  acepta nuevos números de 1 a 99; la API sigue aceptando cualquier entero positivo. Definir
  si el límite debe aplicarse también a la API queda pendiente de decisión de producto.
- `numero` es único entre todas las zonas, incluso para mesas deshabilitadas. Crear o cambiar
  a un número existente responde **409**. `DELETE /api/v1/mesas/{id}` deshabilita la mesa:
  conserva `id` y `numero`, que se pueden volver a usar habilitando el mismo registro.
- La web conserva la edición de mesas anteriores con `numero > 99` si no se renumeran.
- `MesaRequest` incluye `zonaId` (opcional). Al crear sin `zonaId` la mesa va a la **primera zona**;
  al actualizar sin `zonaId` conserva la que tiene.
- `MesaResponse` incluye `zonaId`, `zonaOrden` y `ubicacion`, que ahora es el **nombre de la zona**
  (se conserva por compatibilidad). El campo de texto libre `ubicacion` ya no se puede enviar.
- `MesaResponse.estado` lo mantiene el backend según la cuenta: pasa a **`OCUPADA`** cuando se abre la
  cuenta de la mesa (primer pedido) y vuelve a **`LIBRE`** cuando la cuenta se cierra, sea por cobro,
  por cierre manual o al cancelar el último borrador. Deshabilitar o habilitar la mesa no cambia su
  estado. `RESERVADA` e `INACTIVA` existen en el enum pero ningún flujo las usa todavía (pendiente de
  decisión). La migración `V23__mesas_ocupadas_backfill.sql` marca como ocupadas las mesas que ya
  tenían una cuenta abierta.
- La migración `V21__zonas_de_mesas.sql` convirtió las ubicaciones existentes en zonas y eliminó la
  columna `ubicacion`; conviene respaldar la base antes de aplicarla (`database/backup.sh`).

### Pedidos de salón: filtro y entrega

- `GET /api/v1/pedidos?mesaId=&estado=` (`pedidos:ver`): con `estado` devuelve solo los pedidos de ese
  estado (p. ej. `LISTO`); sin `estado` devuelve todos menos los anulados.
- `POST /api/v1/pedidos/{codigo}/entregar` (`pedidos:entregar`): el mesero marca **servido** un pedido que
  cocina dejó `LISTO`. Pasa a `ENTREGADO`; repetir la llamada responde 200 sin cambios; si el pedido no
  está `LISTO` responde **422**.

### Comandas para la cocina

- `GET /api/v1/comandas?areaId=&estado=&activas=` (`comandas:ver`). Con `activas=true` devuelve lo que la
  cocina necesita ver: todas las `PENDIENTE` y `EN_PREPARACION`, más las `LISTO` modificadas en las últimas
  **2 horas** (ventana fijada en el código; *pendiente de validar con el negocio*). Con `activas=true` el
  parámetro `estado` no se usa. Sin él, el listado no tiene límite.
- Cada comanda incluye, además de sus líneas: `mesaNumero` (solo en pedidos de salón), `canal`
  (`SALON` o `APP`) y `entrega` (`RETIRAR` o `DOMICILIO`, solo en pedidos de la app). Los campos nulos se
  omiten del JSON.
- `POST /api/v1/comandas/{id}/en-preparacion` (`pedidos:estado-preparacion`) y
  `POST /api/v1/comandas/{id}/listo` (`pedidos:estado-listo`). Una comanda de **orden** debe pasar por
  preparación antes de `LISTO`; una de **cancelación** (generada al aprobar una anulación) se da por
  enterada pasando directamente de `PENDIENTE` a `LISTO`. El pedido avanza cuando todas sus áreas llegan
  al estado. Ambas llamadas son idempotentes.

### Pedidos de la app en el mostrador (retiro)

Mesero o cocina entregan al cliente su pedido de retiro, **sin PIN** (el cliente está presente):

- `POST /api/v1/clientes/pedidos/{codigo}/entregar` (`pedidos:retiro-mostrador`): un pedido de `RETIRAR` que
  está `LISTO` pasa a `ENTREGADO` y se avisa al cliente por su canal en vivo. Idempotente. Un pedido a
  **domicilio** responde **422** ("El domicilio lo entrega el repartidor"), igual que uno que aún no
  está listo.
- `GET /api/v1/clientes/operacion/pedidos?estado=LISTO&entrega=RETIRAR` (`pedidos:retiro-mostrador`; ambos
  parámetros son obligatorios): lista, del más antiguo al más reciente, `{codigo, estado, entrega,
  clienteNombre, lineas:[{nombreProducto, cantidad}]}`. No incluye datos de pago ni de entrega. Un estado
  o una forma de entrega no válidos responden **422**.
- Pendiente de decisión: la entrega de los pedidos **a domicilio** (la hace el repartidor) y el cobro de
  los pedidos de la app.

### Imágenes y videos del catálogo

`POST /api/v1/productos/{id}/imagen` recibe `multipart/form-data` con el campo
`archivo`. Requiere el permiso `catalogo:gestionar`; admite imágenes o videos de
hasta 50 MB. El backend guarda el archivo en el servidor WebDAV configurado y
persiste la URL pública en `producto.imagenUrl`. Los espacios y caracteres
especiales del nombre del archivo se codifican en la URL generada.

Las URLs históricas `GET/HEAD /api/v1/archivos/{id}/contenido` se mantienen para
archivos vinculados antes del cambio de almacenamiento y redirigen a la base
pública indicada por `ARCHIVOS_LECTURA_URL`.

Configura `APP_MEDIA_UPLOAD_URL`, `APP_MEDIA_PUBLIC_URL`, `APP_MEDIA_USUARIO` y
`APP_MEDIA_CLAVE` únicamente en el entorno del backend. El navegador sube el
archivo a la API; las credenciales del servidor de archivos nunca se envían al
cliente.

## Roles

Los roles viven en categorías y lo que el panel puede hacer con cada uno son
**banderas de la base de datos** (`roles.asignable_por_admin`, `roles.configurable`,
`roles.origen`), no listas en el código.

| Categoría | Subroles | Asignable | Configurable | Origen |
|---|---|---|---|---|
| `ADMIN` | — | no | no | `ADMIN` |
| `EMPLEADO` | `MESERO`, `CAJERO`, `COCINA` | sí | sí | `ADMIN` |
| `CLIENTE` | — | no | no | `AUTO_REGISTRO` |
| `DELIVERY` | — | no | no | `AUTO_REGISTRO` |

- `GET /api/v1/usuarios/roles` → taxonomía: `{categorias:[{codigo,descripcion,roles[]}], roles[]}`.
  Cada rol trae `categoria`, `asignablePorAdmin`, `configurable`, `origen` y sus `permisos` por defecto.
- `GET /api/v1/usuarios/permisos` → catálogo de funcionalidades para el checklist del panel.
- El administrador asigna funcionalidades por usuario solo en los roles **configurables**
  (`MESERO`, `CAJERO`, `COCINA`); si el usuario no tiene permisos explícitos, rigen los de su rol.
- Crear o cambiar a un rol **no asignable** se rechaza con **422**
  (`urn:problem:restaurante:regla-de-negocio`); un rol no configurable con permisos
  explícitos, también.
- **Permisos de operación por defecto** (migración `V22`):
  - `COCINA`: `catalogo:ver`, `comandas:ver`, `comandas:reenviar`, `pedidos:estado-preparacion`,
    `pedidos:estado-listo` y `pedidos:retiro-mostrador`. No cobra, no ve caja ni reportes y no crea pedidos.
  - `MESERO`: además de los que ya tenía, `pedidos:estado-preparacion`, `pedidos:entregar`,
    `pedidos:retiro-mostrador` y `comandas:reenviar`: puede hacer las mismas acciones que la cocina.
  - Los usuarios con permisos **explícitos** (p. ej. cocineros que la migración `V10` dejó como `MESERO`) no
    reciben estos permisos solos: el administrador debe marcárselos o reasignarlos a `COCINA`. Los permisos
    de un rol llegan a una sesión abierta cuando renueva su token o vuelve a iniciar sesión.
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
