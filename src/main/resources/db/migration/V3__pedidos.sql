-- V3: Pedidos presenciales (borrador, items y confirmación).
-- Regla de precios congelados: cada línea guarda nombre, precio unitario y
-- extras tal como estaban al confirmar (NUMERIC(12,2), siempre BigDecimal).
-- Regla de concurrencia: @Version optimista sobre pedidos.
-- Idempotencia de la confirmación: restricción única pedido + idempotency-key.

CREATE TABLE pedidos (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40)  NOT NULL UNIQUE,
    mesa_id     BIGINT       REFERENCES mesas (id),
    estado      VARCHAR(20)  NOT NULL DEFAULT 'BORRADOR'
                CHECK (estado IN ('BORRADOR', 'CONFIRMADO', 'EN_PREPARACION',
                                  'LISTO', 'ENTREGADO', 'ANULADO')),
    notas       VARCHAR(500),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

CREATE TABLE pedido_lineas (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT       NOT NULL REFERENCES pedidos (id) ON DELETE CASCADE,
    producto_id     BIGINT       NOT NULL,
    nombre_producto VARCHAR(120) NOT NULL,
    precio_unitario NUMERIC(12,2) NOT NULL CHECK (precio_unitario >= 0),
    cantidad        INT          NOT NULL CHECK (cantidad > 0),
    observaciones   VARCHAR(500),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50)
);

CREATE TABLE pedido_linea_extras (
    id          BIGSERIAL PRIMARY KEY,
    linea_id    BIGINT       NOT NULL REFERENCES pedido_lineas (id) ON DELETE CASCADE,
    extra_id    BIGINT       NOT NULL,
    nombre_extra VARCHAR(80) NOT NULL,
    precio      NUMERIC(12,2) NOT NULL CHECK (precio >= 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE pedido_linea_ingredientes (
    id         BIGSERIAL PRIMARY KEY,
    linea_id   BIGINT      NOT NULL REFERENCES pedido_lineas (id) ON DELETE CASCADE,
    nombre     VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Registro de confirmaciones: permite reintentar confirmar sin duplicar la
-- comanda/impresión (RNF-17).
CREATE TABLE confirmaciones (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT       NOT NULL REFERENCES pedidos (id) ON DELETE CASCADE,
    idempotency_key VARCHAR(100) NOT NULL,
    confirmado_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (pedido_id, idempotency_key)
);

CREATE INDEX idx_pedidos_mesa ON pedidos (mesa_id);
CREATE INDEX idx_pedidos_estado ON pedidos (estado);
CREATE INDEX idx_lineas_pedido ON pedido_lineas (pedido_id);
CREATE INDEX idx_linea_extras_linea ON pedido_linea_extras (linea_id);
CREATE INDEX idx_confirmaciones_pedido ON confirmaciones (pedido_id);