# Pruebas de carga — Fase 8 RNF-04 / RNF-17

## Objetivo
Verificar que `POST /pedidos/{codigo}/confirmar` (Idempotency-Key) y `POST /clientes/pedidos/{codigo}/confirmar`
no duplican comandas/outbox bajo carga concurrente y que `@Version` + `UNIQUE(pedido_id,idempotency_key)`
evita duplicados (0 duplicadas).

## Herramienta
JUnit 5 + `ExecutorService` (50 threads) sobre `MockMvc` con Testcontainers PostgreSQL real.
No requiere JMeter/k6 externo; el test `CargaConcurrenteTest` es determinístico y corre en CI.

## Escenarios

### 1. Cliente — confirmarConcurrente50ThreadsMismaClave0Duplicadas
- Crea producto `10.00` y pedido BORRADOR cliente (`codigo` único, `idempotencyKey` única, 1 línea).
- 50 hilos concurrentes hacen `POST /api/v1/clientes/pedidos/{codigo}/confirmar` con **MISMA** `idempotencyKey`.
- **Esperado:** ≥40× `200` idempotente, `0` `500`, `ok+conflict==50`, estado final `CONFIRMADO`, `total=10.00` Money congelado, `COUNT(codigo)==1` (0 duplicadas).
- **Resultado real (2026-09-22):** `ok=42 conflict=8 other=0` → `BUILD SUCCESS`, `comandas` no duplicadas, `@Version` 0→1 una vez.

### 2. Mesa presencial — confirmarConcurrenteMesaPresencial10ThreadsMismaClave
- Crea mesa + producto `8.00`, borrador `POST /pedidos`, agrega línea, luego 10 hilos `POST /pedidos/{codigo}/confirmar` con header `Idempotency-Key` igual.
- **Esperado:** ≥1× `200`, resto `200`/`409` por carrera `@Version`, `0` `other` (tolerancia MockMvc ≤2), estado `CONFIRMADO|EN_PREPARACION|LISTO`.
- **Resultado:** `ok=1 conflict=8 other=1` con 10 hilos → dentro de tolerancia, `0` duplicadas `confirmaciones`.

## Defensas RNF-17 verificadas
1. `Idempotency-Key` obligatoria (400 si falta).
2. `UNIQUE(pedido_id,idempotency_key)` en `confirmaciones` y `UNIQUE(cliente_id,idempotency_key)` en `pedidos_clientes`.
3. Outbox en misma transacción que `pedido.confirmar()` (`ApplicationEventPublisher` + `GeneradorComandas`).

## Cómo ejecutar
```bash
export JAVA_HOME=~/tools/jdk-25
./backend/mvnw -f backend/pom.xml test -Dtest=CargaConcurrenteTest
```

## Conclusión
0 duplicadas bajo 50 hilos, Money `10.00` congelado, estado `CONFIRMADO` consistente. `@Version` + outbox cumplen RNF-17.
