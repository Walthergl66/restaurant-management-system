-- =====================================================================
-- Schema: AUDITORIA
-- Responsabilidad: dejar constancia y entregar eventos de forma fiable,
-- sin participar en el cálculo del negocio. `auditoria_eventos` es de
-- SOLO INSERCIÓN (se revoca UPDATE/DELETE al rol de la aplicación) y
-- `outbox` es la cola at-least-once que alimenta la impresora local y los
-- avisos WebSocket al cliente. Si este schema se pierde, la operación
-- sigue siendo consistente: por eso vive aparte de operaciones/tesoreria.
--
-- Tablas: auditoria_eventos, outbox.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS auditoria;

CREATE TABLE auditoria.auditoria_eventos (
    id         BIGSERIAL PRIMARY KEY,
    usuario    VARCHAR(50)  NOT NULL,
    tipo       VARCHAR(40)  NOT NULL,
    entidad    VARCHAR(40)  NOT NULL,
    entidad_id VARCHAR(50),
    detalle    JSONB,
    fecha      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_auditoria_entidad ON auditoria.auditoria_eventos (entidad, entidad_id);
CREATE INDEX idx_auditoria_fecha   ON auditoria.auditoria_eventos (fecha);

-- Solo-inserción: el rol de la aplicación nunca actualiza ni borra aquí.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'restaurante'
               AND NOT rolsuper) THEN
        EXECUTE 'REVOKE UPDATE, DELETE ON auditoria.auditoria_eventos FROM restaurante';
    END IF;
END $$;

-- Outbox: outbox en la MISMA transacción que la confirmación del pedido;
-- el agente local (impresión) y el difusor de estados lo consumen y lo
-- marcan ENVIADO/FALLIDO con versión optimista.
CREATE TABLE auditoria.outbox (
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
    procesada_at   TIMESTAMPTZ,
    version        BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_estado ON auditoria.outbox (estado);
