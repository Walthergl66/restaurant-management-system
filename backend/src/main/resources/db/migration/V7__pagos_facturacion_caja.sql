-- V7: Cobro, facturación y caja (Fase 6 del plan: RF-26 a RF-35).
--
--  Cobro (RF-26, RF-31): un pago por método (pago mixto permitido) sobre una
--  cuenta abierta; el total de pagos debe coincidir con el total de la cuenta.
--  Facturación (RF-28 a RF-30): interfaz de emisión + emisor interno con
--  numeración secuencial (SRI llega después como otra implementación).
--  Caja (RF-32 a RF-35): una sola caja abierta a la vez (índice único parcial),
--  movimientos de ingreso por cobros y egresos manuales, cierre con diferencias.
--  RNF-11: @Version optimista en cajas.

CREATE TABLE cajas (
    id               BIGSERIAL PRIMARY KEY,
    estado           VARCHAR(20)  NOT NULL DEFAULT 'ABIERTA'
                     CHECK (estado IN ('ABIERTA', 'CERRADA')),
    apertura_inicial NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (apertura_inicial >= 0),
    cierre_esperado  NUMERIC(12,2),
    cierre_real      NUMERIC(12,2),
    diferencia       NUMERIC(12,2),
    abierta_por      VARCHAR(50),
    cerrada_por      VARCHAR(50),
    abierta_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    cerrada_at       TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       VARCHAR(50),
    updated_by       VARCHAR(50)
);

-- Una sola caja abierta a la vez en todo el establecimiento.
CREATE UNIQUE INDEX uq_caja_abierta ON cajas (estado) WHERE estado = 'ABIERTA';

-- Pagos de cuentas (uno por método; el cobro puede ser mixto).
CREATE TABLE pagos (
    id          BIGSERIAL PRIMARY KEY,
    cuenta_id   BIGINT       NOT NULL REFERENCES cuentas (id),
    caja_id     BIGINT       NOT NULL REFERENCES cajas (id),
    metodo      VARCHAR(20)  NOT NULL CHECK (metodo IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'OTRO')),
    monto       NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    cobrado_por VARCHAR(50),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);
CREATE INDEX idx_pagos_cuenta ON pagos (cuenta_id);
CREATE INDEX idx_pagos_caja ON pagos (caja_id);

CREATE TABLE caja_movimientos (
    id         BIGSERIAL PRIMARY KEY,
    caja_id    BIGINT      NOT NULL REFERENCES cajas (id),
    tipo       VARCHAR(10) NOT NULL CHECK (tipo IN ('INGRESO', 'EGRESO')),
    concepto   VARCHAR(120) NOT NULL,
    monto      NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    metodo     VARCHAR(20) CHECK (metodo IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'OTRO')),
    pago_id    BIGINT      REFERENCES pagos (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);
CREATE INDEX idx_caja_movimientos_caja ON caja_movimientos (caja_id);

-- Comprobantes con numeración secuencial global (emisor interno; SRI después).
CREATE SEQUENCE seq_correlativo_comprobante START 1;

CREATE TABLE comprobantes (
    id                    BIGSERIAL PRIMARY KEY,
    correlativo           VARCHAR(20)  NOT NULL UNIQUE,
    secuencial            BIGINT       NOT NULL UNIQUE,
    tipo                  VARCHAR(10)  NOT NULL CHECK (tipo IN ('FACTURA', 'TICKET')),
    cuenta_id             BIGINT       NOT NULL REFERENCES cuentas (id),
    fecha                 TIMESTAMPTZ  NOT NULL DEFAULT now(),
    cliente_nombre        VARCHAR(120),
    cliente_identificacion VARCHAR(20),
    total                 NUMERIC(12,2) NOT NULL,
    emitido_por           VARCHAR(50),
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by            VARCHAR(50),
    updated_by            VARCHAR(50)
);
CREATE INDEX idx_comprobantes_cuenta ON comprobantes (cuenta_id);
CREATE INDEX idx_comprobantes_fecha ON comprobantes (fecha);