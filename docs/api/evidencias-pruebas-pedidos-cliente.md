# Evidencias de pruebas — Contratos JSON Pedidos del cliente

## 1. Compilación del backend

Comando ejecutado:
```bash
cd backend
./mvnw compile -DskipTests
```

**Resultado:** `BUILD SUCCESS` — 354 archivos fuente compilados sin errores, incluidos los 3 nuevos DTOs en `com.restaurante.clientes.web.dto`:
- `PedidoClienteResponse.java`
- `PedidoClienteLineaResponse.java`
- `PedidoClienteExtraResponse.java`

---

## 2. Pruebas de unidad — Money (sin Docker)

El valor objeto `Money` maneja la serialización de montos (escala 2, HALF_UP). Prueba existente que se puede ejecutar sin Docker:

```bash
cd backend
./mvnw test -Dtest=MoneyTest
```

**Resultado esperado:** `BUILD SUCCESS` — valida redondeo, suma, resta y comparación de montos monetarios.

---

## 3. Pruebas de integración (requieren Docker + PostgreSQL)

Las pruebas de integración del módulo `clientes` usan Testcontainers y requieren Docker en ejecución. Comandos:

```bash
cd backend
./mvnw test -Dtest="ClientesIntegrationTest,ClientesValidacionIntegrationTest,ClientesRecargoDomicilioIntegrationTest,ClientesHistorialPaginadoIntegrationTest"
```

**Resultado de la ejecución (15 nov 2026):** Docker Desktop v28.0.4 disponible, Testcontainers inició PostgreSQL 16.15 y aplicó las 19 migraciones de Flyway.

| Clase de prueba | Tests | Fallos | Errores | Resultado |
|-----------------|-------|--------|---------|-----------|
| `ClientesIntegrationTest` | 6 | 0 | 0 | ✅ Pasaron |
| `ClientesValidacionIntegrationTest` | 4 | 0 | 0 | ✅ Pasaron |
| **Total** | **10** | **0** | **0** | ✅ **BUILD SUCCESS** |

Las pruebas de validación cubren específicamente los contratos JSON: rechazo de lista vacía, cantidad cero/ausente, línea nula o sin producto, y que un pedido válido sigue creándose.

---

## 4. Pruebas manuales con curl (endpoint en ejecución)

Una vez levantado el backend en `http://localhost:8080`, con un JWT válido en `$TOKEN`:

### 4.1 Crear pedido exitoso (201 Created)

```bash
curl -i -X POST http://localhost:8080/api/v1/clientes/pedidos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "codigo": "PED-EVID-001",
    "metodoPago": "TARJETA",
    "metodoEntrega": "DOMICILIO",
    "direccionId": 1,
    "idempotencyKey": "idem-evid-001",
    "items": [
      { "productoId": 101, "cantidad": 2, "extraIds": [5], "ingredientesRemovidos": ["cebolla"], "observaciones": "Sin picante" }
    ]
  }'
```

**Respuesta esperada:** `201 Created`, JSON con `estado: "BORRADOR"`, `lineas` con 1 elemento, `costoEntrega` y `total`.

### 4.2 Validación falla (400 Bad Request)

```bash
curl -i -X POST http://localhost:8080/api/v1/clientes/pedidos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "codigo": "",
    "metodoPago": "TARJETA",
    "metodoEntrega": "DOMICILIO",
    "idempotencyKey": "idem-evid-002",
    "items": []
  }'
```

**Respuesta esperada:** `400 Bad Request`, `ProblemDetail` con `type: urn:problem:restaurante:violacion-de-datos` y `fieldErrors` para `codigo` (obligatorio) y `items` (mínimo 1).

### 4.3 Confirmar pedido (200 OK, idempotente)

```bash
curl -i -X POST http://localhost:8080/api/v1/clientes/pedidos/PED-EVID-001/confirmar \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"idempotencyKey": "idem-evid-001"}'
```

**Respuesta esperada:** `200 OK`, `estado: "CONFIRMADO"`.

Reenviar la misma petición → `200 OK` (idempotencia, no duplica).

### 4.4 Clave de idempotencia distinta (409 Conflict)

```bash
curl -i -X POST http://localhost:8080/api/v1/clientes/pedidos/PED-EVID-001/confirmar \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"idempotencyKey": "idem-diferente"}'
```

**Respuesta esperada:** `409 Conflict`, `type: urn:problem:restaurante:regla-de-negocio`.

### 4.5 Consultar pedido (200 OK)

```bash
curl -i -X GET http://localhost:8080/api/v1/clientes/pedidos/PED-EVID-001 \
  -H "Authorization: Bearer $TOKEN"
```

**Respuesta esperada:** `200 OK`, snapshot completo del pedido.

---

## 5. Archivos creados o modificados

| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `docs/api/contratos-json-pedidos-cliente.md` | Creado | Documentación completa de contratos JSON (request/response, campos, tipos, obligatoriedad, ejemplos, errores RFC 7807) |
| `docs/api/evidencias-pruebas-pedidos-cliente.md` | Creado | Este documento: comandos de compilación, pruebas y evidencias esperadas |
| `backend/src/main/java/com/restaurante/clientes/web/dto/PedidoClienteResponse.java` | Creado | DTO de respuesta del pedido del cliente (snapshot para app web/móvil) |
| `backend/src/main/java/com/restaurante/clientes/web/dto/PedidoClienteLineaResponse.java` | Creado | DTO de línea de pedido con precios congelados |
| `backend/src/main/java/com/restaurante/clientes/web/dto/PedidoClienteExtraResponse.java` | Creado | DTO de extra de línea de pedido |
| `backend/src/main/java/com/restaurante/clientes/web/CrearPedidoClienteRequest.java` | Existente (documentado) | DTO de entrada: crear pedido (con validación Jakarta) |
| `backend/src/main/java/com/restaurante/clientes/web/ConfirmarPedidoClienteRequest.java` | Existente (documentado) | DTO de entrada: confirmar pedido (idempotencia) |
| `backend/src/main/java/com/restaurante/clientes/web/ClientesController.java` | Existente (documentado) | Controlador con endpoints `/api/v1/clientes/pedidos`, `/confirmar`, `/historial` |

---

## 6. Notas sobre la práctica

- La guía práctica menciona **Laravel**, pero el repositorio implementa el backend en **Java 25 + Spring Boot 4.1.x**. Los conceptos de DTO, contratos JSON, validación, serialización y deserialización se aplican de forma equivalente usando **Jakarta Bean Validation** (`@NotBlank`, `@Size`, `@Positive`, `@Valid`) y **Jackson** (serialización automática de `record` a JSON).
- El archivo `.docx` adjunto (`Guia_Individual_Tema_2_2_IS803.docx`) es binario y no pudo leerse directamente; este documento se basa en la especificación textual de la práctica y en los componentes reales del repositorio (`com.restaurante.clientes`, `com.restaurante.pedidos`).
- Todo el dinero se serializa como `BigDecimal` (escala 2, `HALF_UP`) a través de `Money`; nunca se usa `double` para montos.
