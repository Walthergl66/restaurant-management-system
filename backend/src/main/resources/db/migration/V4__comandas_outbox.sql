-- V4: Comandas por área y órdenes de impresión (patrón outbox).
--
-- Defensas contra comandas duplicadas (RNF-17), juntas:
--  1. Confirmar pide Idempotency-Key (Fase 3).
--  2. Restricción única (pedido_codigo, area_id).
--  3. El outbox se escribe en la misma transacción que la confirmación:
--     nunca hay un pedido confirmado sin sus comandas ni sus órdenes de impresión.

CREATE SEQUENCE IF NOT EXISTS seq_numero_comanda START 1;

CREATE TABLE comandas (
    id             BIGSERIAL PRIMARY KEY,
    pedido_codigo  VARCHAR(40) NOT NULL,
    numero_comanda INT         NOT NULL,
    area_id        BIGINT      NOT NULL REFERENCES areas (id),
    area_nombre    VARCHAR(80) NOT NULL,
    estado         VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                   CHECK (estado IN ('PENDIENTE', 'EN_PREPARACION', 'LISTO')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50),
    CONSTRAINT uq_comanda_pedido_area UNIQUE (pedido_codigo, area_id)
);

CREATE TABLE comanda_lineas (
    id              BIGSERIAL PRIMARY KEY,
    comanda_id      BIGINT       NOT NULL REFERENCES comandas (id) ON DELETE CASCADE,
    producto_id     BIGINT       NOT NULL,
    nombre_producto VARCHAR(120) NOT NULL,
    cantidad        INT          NOT NULL CHECK (cantidad > 0),
    extras          VARCHAR(300),
    ingredientes    VARCHAR(300),
    observaciones   VARCHAR(500),
    orden           INT          NOT NULL DEFAULT 0
);

-- Órdenes de impresión: el agente local las consume y las marca.
CREATE TABLE outbox (
    id             BIGSERIAL PRIMARY KEY,
    tipo           VARCHAR(40)  NOT NULL,
    agregado_id    VARCHAR(64)  NOT NULL,
    pedido_codigo  VARCHAR(40),
    numero_comanda INT,
    area_id        BIGINT,
    area_nombre    VARCHAR(80),
    evento         VARCHAR(80)  NOT NULL,
    estado         VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE'
                   CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'FALLIDO')),
    intentos       INT          NOT NULL DEFAULT 0,
    error          VARCHAR(500),
    creada_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    procesada_at   TIMESTAMPTZ
);

CREATE INDEX idx_comandas_estado ON comandas (estado);
CREATE INDEX idx_comandas_pedido ON comandas (pedido_codigo);
CREATE INDEX idx_comanda_lineas_comanda ON comanda_lineas (comanda_id);
CREATE INDEX idx_outbox_estado ON outbox (estado);