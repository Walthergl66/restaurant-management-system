# Mejoras pendientes — auditoría de backend

Fuente: auditoría del 30-sep-2026 sobre `backend/`.
Orden de severidad: primero lo que mueve dinero o pierde datos.

| # | Severidad | Estado | Pieza |
|---|---|---|---|
| 1 | ALTA | Pendiente | `cuentas` / `pedidos` |
| 2 | ALTA | **Hecha** (`f766293`) | `mesas` |
| 3 | ALTA | **Hecha** (`27bec62`) | `comandas` |
| 4 | ALTA | Pendiente | `comandas` |
| 5 | MEDIA | **Hecha** (`153a73f`) | `comandas` |
| 6 | MEDIA | Pendiente | `comandas` / `clientes` |
| 7 | MEDIA | Pendiente | `clientes` |
| 8 | MEDIA | Pendiente | `auditoria` / `pagos` |

---

## 1. ALTA — El total de una cuenta suma todos los pedidos confirmados de la mesa, para siempre

**Dónde:** `cuentas/application/CuentaService.java:163` · `pedidos/application/PedidoService.java:224-229` · `db/migration/V3__pedidos.sql:7-20` · `pedidos/domain/Pedido.java:128`

**Problema.** `pedidos` no tiene `cuenta_id`. La pertenencia pedido→cuenta es implícita y la cuenta se totaliza con `pedidosConfirmadosDeMesa(mesaId)` → `findByMesaIdAndEstadoNot(mesaId, ANULADO)`, es decir **todo pedido confirmado de la mesa, en cualquier turno**. `Pedido.entregar()` existe pero ningún endpoint lo invoca, así que un pedido ya cobrado queda `CONFIRMADO`/`LISTO` y se vuelve a sumar.

**Escenario.** Turno 1, mesa 5: pedido `P1` de 30.00 → cobrar → cuenta `C1` CERRADA, mesa LIBRE. Turno 2, mesa 5: pedido `P2` de 25.00 → se abre `C2`. Al cobrar `C2`, `resumen(C2)` devuelve {P1, P2} → total **55.00**. Se emite comprobante por 55.00 y la caja registra un INGRESO de 55.00: el cierre de caja nunca cuadra y la segunda mesa paga la primera. Lo mismo en `anulaciones.aprobadasDe(codigos)` y en `GET /api/v1/cuentas/{id}` de cuentas ya cerradas. Los tests en verde no lo detectan porque todas las suites crean una mesa nueva por test.

**Fix sugerido.** Migración aditiva `V13__pedidos_cuenta.sql`: `ALTER TABLE pedidos ADD COLUMN cuenta_id BIGINT REFERENCES cuentas (id)` + índice. Asignar `cuenta_id` al confirmar (o al abrir la cuenta en `CuentaService.abrirParaMesa`, vía un método nuevo en la API pública `Pedidos`, p. ej. `asignarCuenta(codigo, cuentaId)`, para no invertir la dirección de dependencia `cuentas → pedidos`). Cambiar `pedidosConfirmadosDeMesa` por `pedidosConfirmadosDeCuenta(cuentaId)` y `anulaciones.aprobadasDe(cuentaId)` en vez de derivar los códigos desde la mesa.

---

## 2. ALTA — `Mesa` era el único agregado mutable sin `@Version` — **HECHA**

**Dónde:** `mesas/domain/Mesa.java` · `db/migration/V12__mesa_version.sql` · `mesas/MesaBloqueoOptimistaTest.java`

**Qué se hizo.** Migración aditiva `V12__mesa_version.sql` (`ADD COLUMN version BIGINT NOT NULL DEFAULT 0`) + `@Version` en `Mesa`. El conflictivo devuelve 409 por `GlobalExceptionHandler`. Test determinista con latch (`MesaBloqueoOptimistaTest`): verificado en rojo sin `@Version` (`exitos=2`), en verde con ella. Commits `dd84996`, `f766293`, `0ef9376`.

---

## 3. ALTA — `GeneradorComandas.normalizar` une los extras con `","` — **HECHA**

**Dónde:** `comandas/application/GeneradorComandas.java`.

**Qué se hizo.** `Collectors.joining(",")` y el separador `|` en la clave de agrupación eran ambiguos: un extra llamado `bebida,grande` y los extras `bebida` + `grande` daban la misma cadena y se fusionaban en una sola línea de comanda (la cocina recibía un plato por dos). Se cambió a `\u001F` (separador de control no imprimible), el mismo patrón que ya usan `PedidoService`, `AnulacionService` y `ReportesService`. Commits `27bec62` + test `6922722` (verificado en rojo antes del fix).

---

## 4. ALTA — `avanzarSiTodoElPedidoAvanza` decide con una lectura sin bloqueo: dos áreas marcadas a la vez dejan el pedido sin avanzar nunca

**Dónde:** `comandas/application/ComandaService.java:124-143` · `comandas/infrastructure/ComandaRepository.java` (`findPorPedido` sin `@Lock`)

**Problema.** El flush automático de Hibernate hace visible el cambio propio, pero `findPorPedido` es un JPQL normal: bajo READ COMMITTED **no ve los commits ajenos**. La regla "el pedido avanza cuando todas sus áreas llegaron al estado" es correcta, pero se evalúa sobre una foto inconsistente.

**Escenario.** Pedido P1 con Cocina y Barra. Dos tablets pulsan "en preparación" en el mismo instante. Tx A: `UPDATE comandas SET estado='EN_PREPARACION' WHERE id=cocina` + flush; lee y ve Cocina=`EN_PREPARACION`, Barra=`PENDIENTE` → `todasPreparando=false` → no avanza. Tx B: lee Cocina=`PENDIENTE` (el UPDATE de A sigue sin commitear), Barra=`EN_PREPARACION` → `todasPreparando=false` → no avanza. Commit de ambas: **las dos áreas están EN_PREPARACION y P1 sigue en CONFIRMADO para siempre**. Sólo se recupera si alguien vuelve a tocar una comanda.

**Fix sugerido.** Serializar la decisión: adquirir antes de mutar un lock pesimista sobre las comandas del pedido en orden determinista (`findPorPedidoParaActualizar` con `@Lock(PESSIMISTIC_WRITE)` y `ORDER BY id`), o sobre la fila del `Pedido`/`PedidoCliente`; o reintento acotado cuando el recuento no cuadre.

---

## 5. MEDIA — Una comanda `CANCELACION` de un pedido ya `LISTO` no se podía marcar — **HECHA**

**Dónde:** `comandas/application/ComandaService.java`.

**Qué se hizo.** `avanzarSiTodoElPedidoAvanza` sale temprano si `comanda.getTipo() != TipoComanda.ORDEN`. Antes, marcar una comanda CANCELACION de un pedido LISTO disparaba `Pedido.marcarEnPreparacion()` → `BusinessRuleException` → 422 y **rollback del propio cambio**, dejando la comanda de cancelación atrapada en PENDIENTE. Commits `153a73f` + test de regresión `04c93ce`.

---

## 6. MEDIA — Los endpoints del agente de impresión no filtran por `tipo`

**Dónde:** `comandas/application/ComandaService.java:96-109` · `comandas/web/ComandaController.java` · `db/migration/V1__usuarios.sql` (MESERO, COCINA y CAJERO tienen `comandas:ver`)

**Problema.** `marcarEnviadaImpresion`/`marcarErrorImpresion` hacen `outboxRepository.findById(id)` sin validar `tipo == "comanda"`. El outbox es **tabla compartida** y también la usan las notificaciones push de la app del cliente (`tipo = "pedido-cliente-estado"`, `ClientesService.notificarEstado`). Los ids son `BIGSERIAL`, enumerables por diferencia.

**Escenario.** Un MESERO (permiso `comandas:ver`, sin permisos de clientes) itera `POST /api/v1/comandas/impresion/{id}/enviado` sobre ids consecutivos. Apunta a eventos `pedido-cliente-estado`: `DifusorEstadoPedidoCliente.difundirPendientes` filtra por `PENDIENTE|FALLIDO` y ya no los ve → **el cliente nunca recibe la notificación de que su pedido pasó a EN_PREPARACION/LISTO** (RF-43 roto). Además `EventoOutbox` no tiene `@Version`, así que dos agentes concurrentes se pisan `intentos` sin avisar.

**Fix sugerido.** Cargar con `findByTipoAndId(GeneradorComandas.TIPO_OUTBOX, id)` y devolver 404 si no existe; y añadir `@Version` a `EventoOutbox` (migración aditiva) para no perder `intentos`.

---

## 7. MEDIA — `resolverClienteId` es check-then-act: las dos primeras peticiones simultáneas de un usuario nuevo chocan

**Dónde:** `clientes/application/ClientesService.java:297-332` · `db/migration/V9__clientes.sql:29` (`usuario_id BIGINT NOT NULL UNIQUE`)

**Problema.** `findByUsuarioId` → si vacío → `new Cliente(...)` → `saveAndFlush`. No hay lock ni `INSERT … ON CONFLICT`. La app móvil abre **carrilera con varias peticiones** (menú, carrito, dirección) y todas llegan por `ClientesController.clienteIdActual()` en paralelo, que es precisamente el primer uso de la cuenta.

**Escenario.** Cliente nuevo se loguea y la app dispara `GET /carrito` y `POST /direcciones` a la vez. Ambas ven `findByUsuarioId` vacío; la segunda `saveAndFlush` se bloquea hasta el commit de la primera y muere por violación de UNIQUE → `DataIntegrityViolationException` → 409. El usuario ve un 409 en vez de un 404 al abrir la app por primera vez, y la dirección no se guarda. Además los bucles anti-colisión de `cedula`/`telefono` salen por `intentos < 5` **dejando el valor duplicado**, que es el mismo 409.

**Fix sugerido.** Tomar un lock pesimista sobre `usuarios` (o `SELECT … FOR UPDATE`) antes del `findByUsuarioId`, y tras el `saveAndFlush` capturar `DataIntegrityViolationException` para releer el cliente ganador. Alternativa: `INSERT … ON CONFLICT (usuario_id) DO NOTHING` + relectura.

---

## 8. MEDIA — La auditoría en `REQUIRES_NEW` confirma cobros que después se revierten

**Dónde:** `auditoria/application/AuditoriaService.java:36-39` · `auditoria/application/EventosAuditables.java:65-87` · `pagos/application/PagoService.java:93-98`

**Problema.** `registrar` corre en `REQUIRES_NEW`, así que el commit de la fila de auditoría es **independiente** del commit del negocio. Cuando el evento es `CuentaCobrada`, el negocio todavía tiene trabajo por hacer que puede fallar: si el `UPDATE ... WHERE version = N` de `cuentas` no casa porque otro cajero ya cerró la cuenta, `PagoService.cobrar` revierte **todo** —pagos, movimientos de caja, comprobante— pero el `INSERT` en `auditoria` ya quedó commiteado.

**Escenario.** Dos cajeros cobran la misma cuenta a la vez. Gana el primero. El segundo recibe 409 (correcto), pero en `/api/v1/auditoria` queda un evento `CUENTA_COBRADA` con `total`, `pago` y `comprobante` de un cobro que **nunca existió**. Al auditar, la cuenta de `auditoria` no cuadra con `pagos`/`comprobantes`, que es exactamente lo que RF-52 debería permitir detectar.

**Fix sugerido.** Publicar `CuentaCobrada` desde un `@TransactionalEventListener(phase = AFTER_COMMIT)` (o `TransactionalOutbox`) y dejar el `REQUIRES_NEW` solo para los eventos de intención (`PEDIDO_CREADO`, `ANULACION_SOLICITADA`).

---

## Notas

- **Modulith:** sin violaciones; todos los imports entre módulos van a los paquetes raíz.
- **Migraciones:** aditivas y ninguna incompatible con una ya aplicada.
- **Entorno de tests:** el daemon de Docker está caído; la suite corre apuntando Testcontainers al socket de podman (`DOCKER_HOST=unix:///run/user/1000/podman/podman.sock`, `TESTCONTAINERS_RYUK_DISABLED=true`).
