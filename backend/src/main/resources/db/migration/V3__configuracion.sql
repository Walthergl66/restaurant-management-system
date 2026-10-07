-- =====================================================================
-- MIGRACIÓN CONSOLIDADA (squash del V1..V16 original).
-- Crea el modelo completo de este schema desde cero: este archivo y sus
-- hermanos V1..V8 son las ÚNICAS migraciones que deben correr en una base
-- nueva. Equivale a schemas/03_configuracion.sql del entregable de la práctica.
-- =====================================================================

-- =====================================================================
-- Schema: CONFIGURACION
-- Responsabilidad: la sintonía fina del sistema. Parámetros genéricos
-- clave/valor (nombre del restaurante, tasa de IVA, moneda) y hardware
-- asociado (impresoras térmicas por área de impresión). Es configuración
-- leída por muchos módulos y escrita pocos veces: separarla evita que un
-- cambio de IVA se mezcle con el tráfico transaccional de operaciones.
--
-- Tablas: parametros, impresoras.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS configuracion;

-- Parámetros genéricos clave/valor.
CREATE TABLE configuracion.parametros (
    id          BIGSERIAL PRIMARY KEY,
    clave       VARCHAR(60) NOT NULL UNIQUE,
    valor       VARCHAR(500) NOT NULL,
    tipo        VARCHAR(20) NOT NULL DEFAULT 'TEXTO'
                CHECK (tipo IN ('TEXTO', 'NUMERICO', 'BOOLEANO')),
    descripcion VARCHAR(200),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

-- Impresoras térmicas: destino de las órdenes de impresión.
CREATE TABLE configuracion.impresoras (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(80)  NOT NULL UNIQUE,
    tipo        VARCHAR(30)  NOT NULL
                CHECK (tipo IN ('TERMICA_RED', 'TERMICA_USB', 'ESCPOS')),
    ip          VARCHAR(45),
    puerto      INT          NOT NULL DEFAULT 9100 CHECK (puerto > 0),
    area        VARCHAR(100),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);
