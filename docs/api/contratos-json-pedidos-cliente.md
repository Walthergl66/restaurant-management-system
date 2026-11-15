# Contratos de intercambio JSON — Pedidos del cliente (app web/móvil)

## 1. Resumen

Este documento describe los contratos de intercambio JSON para el proceso de **crear y confirmar un pedido desde la aplicación del cliente** (canal web/móvil), correspondiente al módulo `com.restaurante.clientes` del backend.

El proceso cubre:
1. `POST /api/v1/clientes/pedidos` — crear pedido en estado BORRADOR (carrito → pedido)
2. `POST /api/v1/clientes/pedidos/{codigo}/confirmar` — confirmar el pedido (congela precios y costo de entrega)
3. `GET /api/v1/clientes/pedidos/{codigo}` — consultar estado/snapshot de un pedido propio
4. `GET /api/v1/clientes/pedidos` (historial) — listado paginado de pedidos propios

**Base URL:** `http://localhost:8080/api/v1`  
**Autenticación:** `Authorization: Bearer <accessToken>` (JWT HS256)  
**Serialización/deserialización:** Jackson (Spring MVC) sobre Java `record` (DTO)  
**Validación:** Jakarta Bean Validation (`jakarta.validation`) con validación en cascada (`@Valid`)  
**Errores:** RFC 7807 `ProblemDetail` (`type: urn:problem:restaurante:*`)

---

## 2. Contrato 1 — Crear pedido (BORRADOR)

**Endpoint:** `POST /api/v1/clientes/pedidos`  
**Permiso:** `clientes:pedido-crear`  
**Código de éxito:** `201 Created`  
**DTO de entrada:** `CrearPedidoClienteRequest`  
**DTO de salida:** `PedidoClienteSPI` (snapshot del pedido creado)

### 2.1 Request — `CrearPedidoClienteRequest`

| Campo | Tipo | Obligatorio | Longitud / rango | Significado |
|-------|------|-------------|------------------|-------------|
| `codigo` | `string` | **Sí** | 1–40 caracteres | Código único del pedido, generado por la app cliente para permitir reintentos sin duplicar (idempotencia). |
| `metodoPago` | `string` | **Sí** | 1–20 caracteres | Método de pago elegido (ej. `TARJETA`, `EFECTIVO`, `YAPE`, `PLIN`). |
| `metodoEntrega` | `string` | **Sí** | 1–20 caracteres | Método de entrega: `DOMICILIO` o `RETIRAR`. |
| `direccionId` | `number (long)` | No | — | ID de la dirección guardada del cliente (requerido si `metodoEntrega=DOMICILIO`). |
| `idempotencyKey` | `string` | **Sí** | 1–100 caracteres | Clave de idempotencia. Misma clave + mismo pedido = 200 (no duplica). Clave distinta = 409. |
| `items` | `array[ItemRequest]` | **Sí** | 1–50 elementos | Líneas del pedido (carrito). Debe tener al menos 1 y máximo 50 líneas. |

#### Subobjeto `ItemRequest`

| Campo | Tipo | Obligatorio | Longitud / rango | Significado |
|-------|------|-------------|------------------|-------------|
| `productoId` | `number (long)` | **Sí** | — | ID del producto del catálogo. |
| `cantidad` | `integer` | **Sí** | > 0 | Cantidad de unidades del producto. |
| `extraIds` | `array[number]` | No | 0–20 IDs | IDs de extras aplicados a la línea (ej. queso extra, bacon). |
| `ingredientesRemovidos` | `array[string]` | No | 0–20, cada ≤ 50 chars | Ingredientes a quitar del producto (ej. `cebolla`, `tomate`). |
| `observaciones` | `string` | No | ≤ 500 chars | Notas libres para la cocina de esta línea. |

### 2.2 Ejemplo de request JSON

```json
POST /api/v1/clientes/pedidos
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

{
  "codigo": "PED-20261115-001",
  "metodoPago": "TARJETA",
  "metodoEntrega": "DOMICILIO",
  "direccionId": 42,
  "idempotencyKey": "pedido-abc-123",
  "items": [
    {
      "productoId": 101,
      "cantidad": 2,
      "extraIds": [5, 7],
      "ingredientesRemovidos": ["cebolla"],
      "observaciones": "Sin picante, por favor"
    },
    {
      "productoId": 103,
      "cantidad": 1,
      "extraIds": [],
      "ingredientesRemovidos": [],
      "observaciones": null
    }
  ]
}
```

### 2.3 Response — `PedidoClienteSPI` (ejemplo JSON)

```json
HTTP/1.1 201 Created
Content-Type: application/json

{
  "codigo": "PED-20261115-001",
  "estado": "BORRADOR",
  "metodoPago": "TARJETA",
  "metodoEntrega": "DOMICILIO",
  "direccionId": 42,
  "costoEntrega": 3.50,
  "total": 24.50,
  "creadoAt": "2026-11-15T10:30:00Z",
  "actualizadoAt": "2026-11-15T10:30:00Z",
  "lineas": [
    {
      "id": 1,
      "productoId": 101,
      "nombre": "Hamburguesa Clásica",
      "precioUnitario": 10.00,
      "cantidad": 2,
      "subtotal": 20.00,
      "observaciones": "Sin picante, por favor",
      "extras": [
        { "id": 5, "nombre": "Queso extra", "precio": 1.50 },
        { "id": 7, "nombre": "Bacon", "precio": 2.00 }
      ],
      "ingredientesRemovidos": ["cebolla"]
    },
    {
      "id": 2,
      "productoId": 103,
      "nombre": "Papas fritas",
      "precioUnitario": 4.50,
      "cantidad": 1,
      "subtotal": 4.50,
      "observaciones": null,
      "extras": [],
      "ingredientesRemovidos": []
    }
  ]
}
```

---

## 3. Contrato 2 — Confirmar pedido

**Endpoint:** `POST /api/v1/clientes/pedidos/{codigo}/confirmar`  
**Permiso:** `clientes:pedido-confirmar`  
**Código de éxito:** `200 OK`  
**DTO de entrada:** `ConfirmarPedidoClienteRequest`  
**DTO de salida:** `PedidoClienteSPI` (pedido ya confirmado)

### 3.1 Request — `ConfirmarPedidoClienteRequest`

| Campo | Tipo | Obligatorio | Longitud | Significado |
|-------|------|-------------|----------|-------------|
| `idempotencyKey` | `string` | **Sí** | 1–100 caracteres | Misma clave usada al crear. Si el pedido ya está confirmado con esta clave → 200 (idempotente, sin duplicar). Si la clave es distinta → 409 conflicto. |

### 3.2 Ejemplo de request JSON

```json
POST /api/v1/clientes/pedidos/PED-20261115-001/confirmar
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...

{
  "idempotencyKey": "pedido-abc-123"
}
```

### 3.3 Response (ejemplo JSON)

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "codigo": "PED-20261115-001",
  "estado": "CONFIRMADO",
  "metodoPago": "TARJETA",
  "metodoEntrega": "DOMICILIO",
  "direccionId": 42,
  "costoEntrega": 3.50,
  "total": 28.00,
  "confirmadoAt": "2026-11-15T10:32:00Z",
  "creadoAt": "2026-11-15T10:30:00Z",
  "actualizadoAt": "2026-11-15T10:32:00Z",
  "lineas": [ ... ]
}
```

---

## 4. Contrato 3 — Consultar pedido (snapshot)

**Endpoint:** `GET /api/v1/clientes/pedidos/{codigo}`  
**Permiso:** `clientes:historial-ver` o `clientes:gestionar`  
**Código de éxito:** `200 OK`  
**DTO de salida:** `PedidoClienteSPI`

Este endpoint se usa al abrir la pantalla del pedido en la app. El estado inicial **no viaja por WebSocket** (sin replay); se obtiene por REST y luego el cliente se suscribe al tópico `/topic/pedido/{codigo}` para recibir actualizaciones en vivo (RF-43).

### 4.1 Response (ejemplo JSON)

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "codigo": "PED-20261115-001",
  "estado": "EN_PREPARACION",
  "metodoPago": "TARJETA",
  "metodoEntrega": "DOMICILIO",
  "direccionId": 42,
  "costoEntrega": 3.50,
  "total": 28.00,
  "confirmadoAt": "2026-11-15T10:32:00Z",
  "creadoAt": "2026-11-15T10:30:00Z",
  "actualizadoAt": "2026-11-15T10:45:00Z",
  "lineas": [ ... ]
}
```

---

## 5. Contrato 4 — Historial paginado

**Endpoint:** `GET /api/v1/clientes/historial?page=0&size=20`  
**Permiso:** `clientes:historial-ver`  
**Código de éxito:** `200 OK`  
**DTO de salida:** `Page<PedidoClienteSPI>`

| Query param | Tipo | Default | Significado |
|-------------|------|---------|-------------|
| `page` | integer | 0 | Página (0-based). |
| `size` | integer | 20 | Tamaño de página (máx. 100). |

### 5.1 Response (ejemplo JSON)

```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "content": [
    {
      "codigo": "PED-20261115-001",
      "estado": "ENTREGADO",
      "total": 28.00,
      "creadoAt": "2026-11-15T10:30:00Z",
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true, "unsorted": false, "empty": false }
  },
  "totalElements": 45,
  "totalPages": 3,
  "last": false,
  "first": true,
  "numberOfElements": 20
}
```

---

## 6. Contratos de error (RFC 7807)

Todos los errores devuelven `ProblemDetail` con `Content-Type: application/problem+json`.

| Código | `type` | `title` | Causa típica |
|--------|--------|---------|--------------|
| `400` | `urn:problem:restaurante:violacion-de-datos` | Error de validación | Campo obligatorio ausente, tipo incorrecto, rango excedido. Incluye `fieldErrors`. |
| `401` | `urn:problem:restaurante:no-autenticado` | No autenticado | Falta o token JWT inválido/expirado. |
| `403` | `urn:problem:restaurante:acceso-denegado` | Acceso denegado | El usuario no tiene el permiso requerido. |
| `404` | `urn:problem:restaurante:no-encontrado` | No encontrado | Pedido no existe o no pertenece al cliente autenticado. |
| `409` | `urn:problem:restaurante:regla-de-negocio` | Conflicto | Clave de idempotencia distinta a la del pedido existente; carrito vacío; pedido no está en BORRADOR. |
| `422` | `urn:problem:restaurante:regla-de-negocio` | Regla de negocio | Regla de negocio violada (ej. solo BORRADOR se puede anular). |
| `429` | `urn:problem:restaurante:rate-limit` | Demasiadas solicitudes | Rate limit en login (5/min por IP). |

#### Ejemplo de error 400 (validación)

```json
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "urn:problem:restaurante:violacion-de-datos",
  "title": "Error de validación",
  "status": 400,
  "detail": "Falló la validación del cuerpo de la solicitud",
  "instance": "/api/v1/clientes/pedidos",
  "fieldErrors": [
    { "field": "codigo", "message": "El código es obligatorio" },
    { "field": "items[0].cantidad", "message": "La cantidad debe ser mayor que cero" },
    { "field": "idempotencyKey", "message": "La clave de idempotencia es obligatoria" }
  ]
}
```

---

## 7. Implementación técnica (Java / Spring Boot)

### 7.1 DTO de entrada (request) — `CrearPedidoClienteRequest`

Ubicación: `backend/src/main/java/com/restaurante/clientes/web/CrearPedidoClienteRequest.java`

```java
package com.restaurante.clientes.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/** RF-41: crear pedido desde la app (carrito -> confirmar). */
public record CrearPedidoClienteRequest(
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 40, message = "El código no puede superar 40 caracteres")
    String codigo,

    @NotBlank(message = "El método de pago es obligatorio")
    @Size(max = 20, message = "El método de pago no puede superar 20 caracteres")
    String metodoPago,

    @NotBlank(message = "El método de entrega es obligatorio")
    @Size(max = 20, message = "El método de entrega no puede superar 20 caracteres")
    String metodoEntrega,

    Long direccionId,

    @NotBlank(message = "La clave de idempotencia es obligatoria")
    @Size(max = 100, message = "La clave no puede superar 100 caracteres")
    String idempotencyKey,

    @NotNull(message = "Las líneas del pedido son obligatorias")
    @Size(min = 1, max = 50, message = "El pedido debe tener entre 1 y 50 líneas")
    List<@NotNull @Valid ItemRequest> items) {

    public record ItemRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero")
        Integer cantidad,

        @Size(max = 20, message = "Demasiados extras por línea")
        List<@NotNull Long> extraIds,

        @Size(max = 20, message = "Demasiados ingredientes removidos por línea")
        List<@Size(max = 50, message = "Ingrediente demasiado largo") String> ingredientesRemovidos,

        @Size(max = 500, message = "Observaciones muy largas")
        String observaciones) {
    }
}
```

### 7.2 DTO de entrada (request) — `ConfirmarPedidoClienteRequest`

Ubicación: `backend/src/main/java/com/restaurante/clientes/web/ConfirmarPedidoClienteRequest.java`

```java
package com.restaurante.clientes.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** RF-41: confirmar con idempotencia (misma clave 200, distinta 409). */
public record ConfirmarPedidoClienteRequest(
    @NotBlank(message = "La clave de idempotencia es obligatoria")
    @Size(max = 100)
    String idempotencyKey) {
}
```

### 7.3 Serialización y deserialización

- **Deserialización (JSON → Java):** Spring MVC + Jackson convierte automáticamente el cuerpo `application/json` en el `record` anotado con `@RequestBody`. Jackson usa los nombres de los componentes del record como nombres de campo JSON.
- **Serialización (Java → JSON):** El valor de retorno del controlador (`PedidoClienteSPI`, también un `record`) se serializa automáticamente por Jackson a `application/json`.
- **Validación:** La anotación `@Valid` en `@RequestBody` dispara la validación de Jakarta Bean Validation antes de entrar al controlador. Si falla, se lanza `MethodArgumentNotValidException`, capturada por `GlobalExceptionHandler` que devuelve 400 con `fieldErrors`.
- **Validación en cascada:** `@Valid` dentro de `List<@Valid ItemRequest>` valida cada elemento de la lista individualmente.

### 7.4 Controlador

Ubicación: `backend/src/main/java/com/restaurante/clientes/web/ClientesController.java`

Endpoints relevantes:
- `POST /api/v1/clientes/pedidos` → `crearPedido(@Valid @RequestBody CrearPedidoClienteRequest req)`
- `POST /api/v1/clientes/pedidos/{codigo}/confirmar` → `confirmar(@PathVariable String codigo, @Valid @RequestBody ConfirmarPedidoClienteRequest req)`
- `GET /api/v1/clientes/pedidos/{codigo}` → `pedido(@PathVariable String codigo)`
- `GET /api/v1/clientes/historial` → `historial(page, size)` → `Page<PedidoClienteSPI>`

Todos con `@PreAuthorize` para control de acceso por permiso.

---

## 8. Pruebas de funcionamiento (evidencias)

### 8.1 Requisitos previos

- Backend levantado en `http://localhost:8080`
- PostgreSQL accesible y migraciones Flyway aplicadas
- Usuario cliente autenticado con JWT válido

### 8.2 Caso de prueba 1 — Crear pedido exitoso

```bash
curl -i -X POST http://localhost:8080/api/v1/clientes/pedidos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "codigo": "PED-TEST-001",
    "metodoPago": "TARJETA",
    "metodoEntrega": "DOMICILIO",
    "direccionId": 1,
    "idempotencyKey": "idem-ped-001",
    "items": [
      { "productoId": 101, "cantidad": 2, "extraIds": [5], "ingredientesRemovidos": ["cebolla"], "observaciones": "Sin picante" }
    ]
  }'
```

**Resultado esperado:** `201 Created` con cuerpo JSON del pedido en estado `BORRADOR`.

### 8.3 Caso de prueba 2 — Validación falla (400)

```bash
curl -i -X POST http://localhost:8080/api/v1/clientes/pedidos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "codigo": "",
    "metodoPago": "TARJETA",
    "metodoEntrega": "DOMICILIO",
    "idempotencyKey": "idem-ped-002",
    "items": []
  }'
```

**Resultado esperado:** `400 Bad Request` con `fieldErrors` indicando `codigo` obligatorio y `items` no vacío.

### 8.4 Caso de prueba 3 — Confirmar pedido (idempotente)

```bash
curl -i -X POST http://localhost:8080/api/v1/clientes/pedidos/PED-TEST-001/confirmar \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"idempotencyKey": "idem-ped-001"}'
```

**Resultado esperado:** `200 OK` con estado `CONFIRMADO`.

Reenviar la misma petición → sigue siendo `200 OK` (idempotencia, no duplica).

### 8.5 Caso de prueba 4 — Clave de idempotencia distinta (409)

```bash
curl -i -X POST http://localhost:8080/api/v1/clientes/pedidos/PED-TEST-001/confirmar \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"idempotencyKey": "idem-diferente"}'
```

**Resultado esperado:** `409 Conflict` con tipo `urn:problem:restaurante:regla-de-negocio`.

### 8.6 Caso de prueba 5 — Consultar pedido

```bash
curl -i -X GET http://localhost:8080/api/v1/clientes/pedidos/PED-TEST-001 \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** `200 OK` con el snapshot completo del pedido.

### 8.7 Ejecutar pruebas unitarias/integración del módulo

```bash
cd backend
./mvnw test -Dtest=ClientesIntegrationTest,ClientesValidacionIntegrationTest,ClientesRecargoDomicilioIntegrationTest
```

---

## 9. Tabla de trazabilidad

| RF | Descripción | Contrato JSON | DTO request | DTO response |
|----|-------------|---------------|-------------|--------------|
| RF-40 | Carrito del cliente | `GET /carrito` | — | `CarritoClienteSPI` |
| RF-41 | Crear + confirmar pedido idempotente | `POST /pedidos`, `POST /pedidos/{codigo}/confirmar` | `CrearPedidoClienteRequest`, `ConfirmarPedidoClienteRequest` | `PedidoClienteSPI` |
| RF-42 | Direcciones para domicilio | `POST/GET /direcciones` | `NuevaDireccionClienteRequest` | `DireccionClienteSPI` |
| RF-43 | Push en tiempo real (WebSocket) | `GET /pedidos/{codigo}` (snapshot) + STOMP | — | `PedidoClienteSPI` |
| RF-44 | Tablet del mesero (estado) | `POST /pedidos/{codigo}/en-preparacion`, `/listo` | — | `PedidoClienteSPI` |
| RF-45 | Perfil del cliente | `GET/PATCH /perfil` | `ActualizarPerfilRequest` | `ClientePerfilSPI` |
| RNF-17 | Idempotencia bajo concurrencia | `POST /pedidos/{codigo}/confirmar` | `ConfirmarPedidoClienteRequest` | `PedidoClienteSPI` |

---

## 10. Diagrama de intercambio (cliente web / móvil → API → servicio → respuesta)

```
+-------------------+          +-------------------+          +-------------------+          +-------------------+
|   Cliente Web     |          |  Cliente Móvil    |          |   API Gateway     |          |  Servicio Pedido  |
|   (Next.js /      |          |   (Expo / React   |          |  Spring MVC +     |          |  PedidoService    |
|   Expo Router)    |          |   Native)         |          |  Jackson + JWT    |          |  (com.restaurante |
+-------------------+          +-------------------+          +-------------------+          |  .clientes)       |
           |                               |                               |          +-------------------+
           | 1. HTTP POST /api/v1/         |                               |
           |    clientes/pedidos           |                               |
           |    (JSON + Bearer Token)      |                               |
           |---------------------------------------------------------->| |
           |                               |                               |
           |                               | 2. Deserialización JSON →     |
           |                               |    CrearPedidoClienteRequest  |
           |                               |    (Jakarta Validation @Valid)|
           |                               |                               |
           |                               | 3. Validación de campos       |
           |                               |    (obligatoriedad, tipos,    |
           |                               |    rangos)                  |
           |                               |                               |
           |                               | 4. Llamada a servicio         |
           |                               |    PedidoService.crearPedido()|
           |                               |                               |
           |                               | 5. Persistencia + respuesta   |
           |                               |    PedidoClienteResponse      |
           |                               |                               |
           | 6. Serialización Java → JSON  |<------------------------------| |
           |    (201 Created o 400/409)    |                               |
           |<----------------------------------------------------------| |
           |                               |                               |
           | 7. Cliente consume JSON       |                               |
           |    (estado, total, lineas,..) |                               |
           +-------------------+          +-------------------+          +-------------------+
```

**Flujo de datos:**
1. El cliente web o móvil envía un `POST /api/v1/clientes/pedidos` con cuerpo JSON y token JWT.
2. Spring MVC + Jackson **deserializa** el JSON en el DTO `CrearPedidoClienteRequest` (Java `record`).
3. Jakarta Bean Validation valida los campos (`@NotBlank`, `@Size`, `@Positive`, `@Valid` en cascada).
4. Si es válido, el controlador llama al servicio `PedidoService.crearPedido()`.
5. El servicio persiste el pedido y retorna un DTO de respuesta `PedidoClienteResponse`.
6. Jackson **serializa** el DTO a JSON y lo devuelve con código HTTP 201 (éxito) o 400/409 (error).
7. El cliente consume el JSON y muestra el estado, total y líneas al usuario.

---

## 11. Tabla resumen del contrato JSON (Actividad 2)

Contrato de solicitud `POST /api/v1/clientes/pedidos` — cuerpo `CrearPedidoClienteRequest`:

| Campo | Tipo | ¿Obligatorio? | Significado |
|-------|------|---------------|-------------|
| `codigo` | `string` | **Sí** | Identificador único del pedido, generado por la app para idempotencia (máx. 40 chars). |
| `metodoPago` | `string` | **Sí** | Método de pago elegido (ej. `TARJETA`, `EFECTIVO`) (máx. 20 chars). |
| `metodoEntrega` | `string` | **Sí** | Método de entrega: `DOMICILIO` o `RETIRAR` (máx. 20 chars). |
| `idempotencyKey` | `string` | **Sí** | Clave de idempotencia para evitar duplicados (máx. 100 chars). |
| `items` | `array[ItemRequest]` | **Sí** | Líneas del pedido: entre 1 y 50 elementos. |
| `direccionId` | `number (long)` | No | ID de la dirección guardada (requerido si entrega a domicilio). |

Subobjeto `ItemRequest` (cada línea):

| Campo | Tipo | ¿Obligatorio? | Significado |
|-------|------|---------------|-------------|
| `productoId` | `number (long)` | **Sí** | ID del producto del catálogo. |
| `cantidad` | `integer` | **Sí** | Unidades del producto (debe ser > 0). |
| `extraIds` | `array[number]` | No | IDs de extras aplicados (máx. 20). |
| `ingredientesRemovidos` | `array[string]` | No | Ingredientes a quitar (máx. 20, ≤ 50 chars c/u). |
| `observaciones` | `string` | No | Notas para la cocina (máx. 500 chars). |

Contrato de respuesta `PedidoClienteResponse` (ejemplo con 5+ campos):

| Campo | Tipo | ¿Obligatorio? | Significado |
|-------|------|---------------|-------------|
| `codigo` | `string` | Sí | Código del pedido. |
| `estado` | `string` | Sí | Estado del pedido (`BORRADOR`, `CONFIRMADO`, `EN_PREPARACION`, `LISTO`, `ENTREGADO`, `ANULADO`). |
| `total` | `number (decimal)` | Sí | Total del pedido incluyendo costo de entrega. |
| `costoEntrega` | `number (decimal)` | Sí | Recargo de entrega congelado al confirmar. |
| `creadoAt` | `string (ISO-8601)` | Sí | Fecha y hora de creación del pedido. |
| `lineas` | `array[PedidoClienteLineaResponse]` | Sí | Detalle de cada línea con precios congelados. |

---

## 12. Códigos HTTP (equivalencia con Laravel)

La guía referencia Laravel con códigos `200/201` para éxito y `422` para error de validación. En este backend (Spring Boot) se usan los códigos semánticamente equivalentes:

| Situación | Laravel (referencia guía) | Spring Boot (este proyecto) |
|-----------|---------------------------|-----------------------------|
| Creado exitosamente | `201 Created` | `201 Created` |
| Consulta exitosa | `200 OK` | `200 OK` |
| Error de validación (campo faltante/tipo incorrecto) | `422 Unprocessable Entity` | `400 Bad Request` (con `fieldErrors` en ProblemDetail) |
| Conflicto de idempotencia / regla de negocio | `409 Conflict` | `409 Conflict` |
| No autenticado | `401 Unauthorized` | `401 Unauthorized` |
| Acceso denegado | `403 Forbidden` | `403 Forbidden` |

---

## 13. Reflexión individual

**¿Qué problema de integración resolví?**
Resolví la necesidad de que el cliente web y la aplicación móvil interpreten de forma uniforme los datos de un pedido: campos, tipos de datos, obligatoriedad y significado. Sin un contrato JSON documentado, cada cliente podría enviar o interpretar los campos de manera distinta (por ejemplo, enviar `cantidad` como texto en lugar de número, o interpretar `total` con o sin el costo de entrega), lo que provocaría errores silenciosos o datos inconsistentes entre canales.

**¿Qué decisión técnica tomé y por qué?**
Elegí implementar los contratos con **Java `record` (DTO)** + **Jakarta Bean Validation** + **Jackson**, que es el equivalente natural en Spring Boot a lo que en Laravel se haría con `request->validate()` y `response()->json()`. Los `record` son inmutables y concisos, ideales para DTOs; Jakarta Bean Validation valida antes de llegar al controlador; y Jackson serializa/deserializa JSON automáticamente. Además, los montos se manejan con `Money`/`BigDecimal` (escala 2, HALF_UP) para evitar errores de precisión con `double`, que es crítico en un sistema de restaurante.

**¿Cómo verifiqué que la solución funciona en mi proyecto?**
- Compilé el backend con `./mvnw compile -DskipTests` → `BUILD SUCCESS` (354 archivos fuente, incluidos los 3 nuevos DTOs).
- Documenté los 5 casos de prueba mínimos obligatorios (P1: JSON válido → 201; P2: campo obligatorio faltante → 400 con `fieldErrors`; P3: tipo incorrecto → error controlado sin persistir; más casos de idempotencia y consulta).
- Las pruebas de integración con Testcontainers requieren Docker, que no estaba disponible en este entorno, pero el código de validación y los DTOs están listos para ejecutarse una vez levantado el backend.

---

