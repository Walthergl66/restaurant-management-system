# Decisión de escalado (plan mejoras 5)

## Contexto

El backend es un monolito modular (Spring Modulith) para un restaurante:
decenas de pedidos diarios, sin autenticación pública masiva. El despliegue
actual va detrás de un proxy TLS con una sola réplica.

## Decisión: una sola instancia (fase actual)

Para el volumen esperado y hasta que se exija alta disponibilidad, se opera
**una sola instancia**. Es coherente con lo implementado:

- `LoginRateLimiter` es local al proceso (in-memory), correcto con una réplica.
- El tortuoso del outbox (lectura de `PENDIENTE`/`FALLIDO` ordenado por fecha)
  no necesita coordinación entre instancias porque solo hay una.
- `sesion_version` y la validación stateless del JWT consultan la BD compartida,
  por lo que ya son coherentes si mañana hay varias réplicas.

## Cuándo migrar a varias instancias

- Se requiere alta disponibilidad (ventana de cero downtime) o despliegues
  azul/verde.
- Picos sostenidos que saturen el pool Hikari o la BD (mida con el pool y
  `management.metrics` / `/actuator/prometheus`).

## Mecanismos para el modo multi-instancia (cuando toque)

1. **Rate limit compartido**: reemplazar `LoginRateLimiter` por un contador
   Redis (ventana fija por IP) o delegar el límite global al gateway/proxy.
2. **Outbox sin doble procesamiento**: reclamar lotes con
   `SELECT ... FOR UPDATE SKIP LOCKED` (marca el lote como "en curso" en la
   misma transacción). Hoy no se necesita: idempotencia por
   `(tipo, agregadoId, evento)` y una sola instancia evitan duplicados.
3. **Pool Hikari y lotes**: probar bajo carga antes de escalar el
   `maximum-pool-size` (prod: 20) y el tamaño de lotes del outbox.

## Comprobaciones al cerrar la fase

- Suite completa en verde con una sola réplica (criterio de la fase actual).
- Cuando se decida escalar, validar con prueba de carga y rate-limit global.