-- =====================================================================
-- MIGRACIÓN CONSOLIDADA (squash del V1..V16 original).
-- Crea el modelo completo de este schema desde cero: este archivo y sus
-- hermanos V1..V8 son las ÚNICAS migraciones que deben correr en una base
-- nueva. Equivale a schemas/06_tesoreria.sql del entregable de la práctica.
-- =====================================================================

-- =====================================================================
-- Schema: TESORERIA
-- Responsabilidad: todo lo que mueve dinero. Cajas (apertura, movimientos
-- de ingreso/egreso, cierre conciliado), pagos de cuentas (uno por método,
-- cobro mixto) y comprobantes fiscales con numeración secuencial única.
-- Agrupadas por compartir el mismo ciclo contable y las mismas garantías
-- (monto > 0, una sola caja abierta, correlativo único): aislarlas
-- permite aplicar sobre este schema los permisos más restrictivos de la
-- base de datos y auditar su acceso sin ruido del resto de operaciones.
--
-- Tablas: cajas, pagos, caja_movimientos, comprobantes.
-- Incluye la secuencia global del correlativo de comprobantes.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS tesoreria;

CREATE SEQUENCE IF NOT EXISTS tesoreria.seq_correlativo_comprobante START 1;

CREATE TABLE tesoreria.cajas (
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
CREATE UNIQUE INDEX uq_caja_abierta
    ON tesoreria.cajas (estado) WHERE estado = 'ABIERTA';

-- Pagos de cuentas (uno por método; el cobro puede ser mixto).
CREATE TABLE tesoreria.pagos (
    id          BIGSERIAL PRIMARY KEY,
    cuenta_id   BIGINT       NOT NULL REFERENCES operaciones.cuentas (id),
    caja_id     BIGINT       NOT NULL REFERENCES tesoreria.cajas (id),
    metodo      VARCHAR(20)  NOT NULL
                CHECK (metodo IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'OTRO')),
    monto       NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    cobrado_por VARCHAR(50),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

CREATE INDEX idx_pagos_cuenta ON tesoreria.pagos (cuenta_id);
CREATE INDEX idx_pagos_caja ON tesoreria.pagos (caja_id);

CREATE TABLE tesoreria.caja_movimientos (
    id         BIGSERIAL PRIMARY KEY,
    caja_id    BIGINT      NOT NULL REFERENCES tesoreria.cajas (id),
    tipo       VARCHAR(10) NOT NULL CHECK (tipo IN ('INGRESO', 'EGRESO')),
    concepto   VARCHAR(120) NOT NULL,
    monto      NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    metodo     VARCHAR(20) CHECK (metodo IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'OTRO')),
    pago_id    BIGINT      REFERENCES tesoreria.pagos (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE INDEX idx_caja_movimientos_caja ON tesoreria.caja_movimientos (caja_id);

-- Comprobantes con numeración secuencial global (emisor interno; SRI después).
CREATE TABLE tesoreria.comprobantes (
    id                     BIGSERIAL PRIMARY KEY,
    correlativo            VARCHAR(20)  NOT NULL UNIQUE,
    secuencial             BIGINT       NOT NULL UNIQUE,
    tipo                   VARCHAR(10)  NOT NULL CHECK (tipo IN ('FACTURA', 'TICKET')),
    cuenta_id              BIGINT       NOT NULL REFERENCES operaciones.cuentas (id),
    fecha                  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    cliente_nombre         VARCHAR(120),
    cliente_identificacion VARCHAR(20),
    total                  NUMERIC(12,2) NOT NULL,
    emitido_por            VARCHAR(50),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by             VARCHAR(50),
    updated_by             VARCHAR(50)
);

CREATE INDEX idx_comprobantes_cuenta ON tesoreria.comprobantes (cuenta_id);
CREATE INDEX idx_comprobantes_fecha ON tesoreria.comprobantes (fecha);
