-- =====================================================================
-- MIGRACIÓN CONSOLIDADA (squash del V1..V16 original).
-- Crea el modelo completo de este schema desde cero: este archivo y sus
-- hermanos V1..V8 son las ÚNICAS migraciones que deben correr en una base
-- nueva. Equivale a schemas/05_comercial.sql del entregable de la práctica.
-- =====================================================================

-- =====================================================================
-- Schema: COMERCIAL
-- Responsabilidad: el canal de venta hacia el cliente final (app con QR,
-- domicilio). Perfil de cliente y direcciones de entrega, y los pedidos
-- propios del cliente con sus líneas congeladas. Separado de
-- `operaciones` porque es otro canal con estados, idempotencia y
-- reglas propias (RF-40 a RF-45); comparte catálogo vía FK
-- (productos/extras en `catalogo`) y reutiliza `seguridad` para el login,
-- pero no toca las tablas del servicio en sala.
--
-- Tablas: clientes, clientes_direcciones, pedidos_clientes,
--         pedidos_clientes_lineas, pedidos_clientes_lineas_extras.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS comercial;

-- Perfil del cliente: ligado 1:1 al usuario logueado de `seguridad`.
CREATE TABLE comercial.clientes (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT      NOT NULL UNIQUE REFERENCES seguridad.usuarios (id),
    cedula          VARCHAR(20) NOT NULL UNIQUE,
    telefono        VARCHAR(20) NOT NULL UNIQUE,
    nombre          VARCHAR(80) NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    creado_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Direcciones del cliente para domicilio.
CREATE TABLE comercial.clientes_direcciones (
    id             BIGSERIAL PRIMARY KEY,
    cliente_id     BIGINT       NOT NULL REFERENCES comercial.clientes (id),
    etiqueta       VARCHAR(40)  NOT NULL,
    direccion      VARCHAR(200) NOT NULL,
    telefono       VARCHAR(20),
    observaciones  VARCHAR(200),
    activa         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Pedido del cliente: BORRADOR -> CONFIRMADO -> EN_PREPARACION -> LISTO
-- -> ENTREGADO (o ANULADO), idempotencia única por cliente.
CREATE TABLE comercial.pedidos_clientes (
    id               BIGSERIAL PRIMARY KEY,
    codigo           VARCHAR(40)  NOT NULL UNIQUE,
    cliente_id       BIGINT       NOT NULL REFERENCES comercial.clientes (id),
    metodo_pago      VARCHAR(20)  NOT NULL
                     CHECK (metodo_pago IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA')),
    metodo_entrega   VARCHAR(20)  NOT NULL
                     CHECK (metodo_entrega IN ('RETIRAR', 'DOMICILIO')),
    direccion_id     BIGINT       REFERENCES comercial.clientes_direcciones (id),
    estado           VARCHAR(20)  NOT NULL
                     CHECK (estado IN ('BORRADOR', 'CONFIRMADO', 'EN_PREPARACION',
                                       'LISTO', 'ENTREGADO', 'ANULADO')),
    idempotency_key  VARCHAR(100) NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    confirmado_at    TIMESTAMPTZ,
    creado_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_pedido_cliente_idempotencia UNIQUE (cliente_id, idempotency_key)
);

-- Líneas congeladas con precio al confirmar (aquí SÍ hay FK a catálogo:
-- la app del cliente confirma en línea, a diferencia del flujo en sala).
CREATE TABLE comercial.pedidos_clientes_lineas (
    id            BIGSERIAL PRIMARY KEY,
    pedido_id     BIGINT        NOT NULL REFERENCES comercial.pedidos_clientes (id),
    producto_id   BIGINT        NOT NULL REFERENCES catalogo.productos (id),
    nombre        VARCHAR(100)  NOT NULL,
    precio        NUMERIC(12,2) NOT NULL,
    cantidad      INT           NOT NULL CHECK (cantidad > 0),
    observaciones VARCHAR(200),
    creado_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE comercial.pedidos_clientes_lineas_extras (
    id        BIGSERIAL PRIMARY KEY,
    linea_id  BIGINT        NOT NULL REFERENCES comercial.pedidos_clientes_lineas (id),
    extra_id  BIGINT        NOT NULL REFERENCES catalogo.extras (id),
    nombre    VARCHAR(100)  NOT NULL,
    precio    NUMERIC(12,2) NOT NULL,
    creado_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_pedcli_cliente ON comercial.pedidos_clientes (cliente_id);
CREATE INDEX idx_pedcli_estado  ON comercial.pedidos_clientes (estado);
