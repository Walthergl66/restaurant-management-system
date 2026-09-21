-- V5: Cuentas, anulaciones y comandas de cancelación
-- (Fase 5 del plan: Bloqueo, adiciones, anulaciones y cuenta, RF-15 a RF-25, RNF-10/11/16).
--
--  Bloqueo (RF-15/16) y adiciones: el bloqueo al confirmar nació en Fase 3;
--  aquí la CUENTA agrupa por mesa todos los pedidos (las adiciones son pedidos
--  nuevos de la misma mesa, RF-17 a RF-19).
--  RNF-16: el total de la cuenta se calcula SIEMPRE desde los registros
--  (pedidos confirmados − anulaciones aprobadas) en una sola clase.
--  Anulación (RF-20 a RF-23): registro aparte SOLICITADA/APROBADA/RECHAZADA;
--  la línea original nunca se borra ni se edita. Al aprobarse se genera la
--  comanda de cancelación y se descuenta de la cuenta.
--  RNF-11: @Version optimista en cuentas y anulaciones.

CREATE TABLE cuentas (
    id          BIGSERIAL PRIMARY KEY,
    mesa_id     BIGINT       NOT NULL REFERENCES mesas (id),
    estado      VARCHAR(20)  NOT NULL DEFAULT 'ABIERTA'
                CHECK (estado IN ('ABIERTA', 'CERRADA')),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

-- Una mesa tiene una sola cuenta abierta a la vez.
CREATE UNIQUE INDEX uq_cuenta_mesa_abierta ON cuentas (mesa_id) WHERE estado = 'ABIERTA';

CREATE TABLE anulaciones (
    id              BIGSERIAL PRIMARY KEY,
    pedido_codigo   VARCHAR(40)    NOT NULL REFERENCES pedidos (codigo),
    linea_id        BIGINT         NOT NULL,
    producto_id     BIGINT         NOT NULL REFERENCES productos (id),
    nombre_producto VARCHAR(120)   NOT NULL,
    precio_unitario NUMERIC(12,2)  NOT NULL CHECK (precio_unitario >= 0),
    cantidad        INT            NOT NULL CHECK (cantidad > 0),
    motivo          VARCHAR(300),
    area_id         BIGINT         REFERENCES areas (id),
    area_nombre     VARCHAR(80),
    estado          VARCHAR(20)    NOT NULL DEFAULT 'SOLICITADA'
                    CHECK (estado IN ('SOLICITADA', 'APROBADA', 'RECHAZADA')),
    solicitado_por  VARCHAR(50),
    resuelto_por    VARCHAR(50),
    resuelta_at     TIMESTAMPTZ,
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50)
);

CREATE INDEX idx_anulaciones_pedido ON anulaciones (pedido_codigo);
CREATE INDEX idx_anulaciones_estado ON anulaciones (estado);

-- Comanda de cancelación: misma tabla con tipo CANCELACION y una sola por
-- anulación aprobada (índice único parcial → idempotente ante reintentos).
ALTER TABLE comandas
    ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'ORDEN'
        CHECK (tipo IN ('ORDEN', 'CANCELACION'));
ALTER TABLE comandas
    ADD COLUMN anulacion_id BIGINT REFERENCES anulaciones (id);

CREATE UNIQUE INDEX uq_comanda_anulacion ON comandas (anulacion_id) WHERE anulacion_id IS NOT NULL;

-- La anti-duplicado de comandas ORDEN (defensa 2 de RNF-17) se conserva solo para
-- las comandas de preparación; una cancelación no compite con el ORDEN del área.
ALTER TABLE comandas DROP CONSTRAINT uq_comanda_pedido_area;
CREATE UNIQUE INDEX uq_comanda_pedido_area_orden
    ON comandas (pedido_codigo, area_id) WHERE tipo = 'ORDEN';