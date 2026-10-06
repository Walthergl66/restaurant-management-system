-- =====================================================================
-- Schema: OPERACIONES
-- Responsabilidad: el ciclo de vida del servicio en sala. Pedidos con sus
-- líneas congeladas (nombre/precio/extras al confirmar), confirmaciones
-- idempotentes, comandas de cocina por área, cuentas de mesa y
-- anulaciones con su registro aparte. Es el schema con más tráfico y con
-- las FK más cruzadas: hacia catalogo (mesas, áreas, productos) y hacia
-- tesoreria (pagos apunta a cuentas desde fuera).
--
-- Tablas: cuentas, pedidos, pedido_lineas, pedido_linea_extras,
--         pedido_linea_ingredientes, confirmaciones, anulaciones,
--         comandas, comanda_lineas.
-- Incluye la secuencia global de numeración de comandas.
--
-- Orden: `cuentas` antes que `pedidos` porque pedidos.cuenta_id la
-- referencia; `anulaciones` antes que `comandas` por comandas.anulacion_id.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS operaciones;

CREATE SEQUENCE IF NOT EXISTS operaciones.seq_numero_comanda START 1;

CREATE TABLE operaciones.cuentas (
    id          BIGSERIAL PRIMARY KEY,
    mesa_id     BIGINT       NOT NULL REFERENCES catalogo.mesas (id),
    estado      VARCHAR(20)  NOT NULL DEFAULT 'ABIERTA'
                CHECK (estado IN ('ABIERTA', 'CERRADA')),
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

-- Una mesa tiene una sola cuenta abierta a la vez.
CREATE UNIQUE INDEX uq_cuenta_mesa_abierta
    ON operaciones.cuentas (mesa_id) WHERE estado = 'ABIERTA';

CREATE TABLE operaciones.pedidos (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(40)  NOT NULL UNIQUE,
    mesa_id     BIGINT       REFERENCES catalogo.mesas (id),
    cuenta_id   BIGINT       REFERENCES operaciones.cuentas (id),
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

CREATE INDEX idx_pedidos_mesa ON operaciones.pedidos (mesa_id);
CREATE INDEX idx_pedidos_estado ON operaciones.pedidos (estado);
CREATE INDEX idx_pedidos_cuenta ON operaciones.pedidos (cuenta_id);

-- Regla de precios congelados: la línea guarda nombre y precio tal como
-- estaban al confirmar; producto_id NO tiene FK (dato congelado a propósito).
CREATE TABLE operaciones.pedido_lineas (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT       NOT NULL REFERENCES operaciones.pedidos (id) ON DELETE CASCADE,
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

CREATE TABLE operaciones.pedido_linea_extras (
    id          BIGSERIAL PRIMARY KEY,
    linea_id    BIGINT       NOT NULL REFERENCES operaciones.pedido_lineas (id) ON DELETE CASCADE,
    extra_id    BIGINT       NOT NULL,
    nombre_extra VARCHAR(80) NOT NULL,
    precio      NUMERIC(12,2) NOT NULL CHECK (precio >= 0),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE operaciones.pedido_linea_ingredientes (
    id         BIGSERIAL PRIMARY KEY,
    linea_id   BIGINT      NOT NULL REFERENCES operaciones.pedido_lineas (id) ON DELETE CASCADE,
    nombre     VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Registro de confirmaciones: permite reintentar confirmar sin duplicar
-- comandas/impresión (idempotency-key única por pedido).
CREATE TABLE operaciones.confirmaciones (
    id              BIGSERIAL PRIMARY KEY,
    pedido_id       BIGINT       NOT NULL REFERENCES operaciones.pedidos (id) ON DELETE CASCADE,
    idempotency_key VARCHAR(100) NOT NULL,
    confirmado_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (pedido_id, idempotency_key)
);

CREATE INDEX idx_lineas_pedido ON operaciones.pedido_lineas (pedido_id);
CREATE INDEX idx_linea_extras_linea ON operaciones.pedido_linea_extras (linea_id);
CREATE INDEX idx_confirmaciones_pedido ON operaciones.confirmaciones (pedido_id);

-- Anulación: registro aparte (SOLICITADA/APROBADA/RECHAZADA); la línea
-- original nunca se borra ni se edita. FK a pedidos por la columna única
-- `codigo` (los pedidos se generan con ID de negocio en la app cliente).
CREATE TABLE operaciones.anulaciones (
    id              BIGSERIAL PRIMARY KEY,
    pedido_codigo   VARCHAR(40)    NOT NULL REFERENCES operaciones.pedidos (codigo),
    linea_id        BIGINT         NOT NULL,
    producto_id     BIGINT         NOT NULL REFERENCES catalogo.productos (id),
    nombre_producto VARCHAR(120)   NOT NULL,
    precio_unitario NUMERIC(12,2)  NOT NULL CHECK (precio_unitario >= 0),
    cantidad        INT            NOT NULL CHECK (cantidad > 0),
    motivo          VARCHAR(300),
    area_id         BIGINT         REFERENCES catalogo.areas (id),
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

CREATE INDEX idx_anulaciones_pedido ON operaciones.anulaciones (pedido_codigo);
CREATE INDEX idx_anulaciones_estado ON operaciones.anulaciones (estado);

-- Comandas: una por área al confirmar (tipo ORDEN) o la de cancelación de
-- una anulación aprobada (tipo CANCELACION). area_nombre y pedido_codigo
-- están congelados; la secuencia global numera todas las comandas.
CREATE TABLE operaciones.comandas (
    id             BIGSERIAL PRIMARY KEY,
    pedido_codigo  VARCHAR(40) NOT NULL,
    numero_comanda INT         NOT NULL,
    area_id        BIGINT      NOT NULL REFERENCES catalogo.areas (id),
    area_nombre    VARCHAR(80) NOT NULL,
    estado         VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                   CHECK (estado IN ('PENDIENTE', 'EN_PREPARACION', 'LISTO')),
    tipo           VARCHAR(20) NOT NULL DEFAULT 'ORDEN'
                   CHECK (tipo IN ('ORDEN', 'CANCELACION')),
    anulacion_id   BIGINT      REFERENCES operaciones.anulaciones (id),
    version        BIGINT      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50)
);

-- Anti-duplicado (RNF-17) solo para comandas de preparación; cada
-- anulación genera a lo sumo una comanda de cancelación.
CREATE UNIQUE INDEX uq_comanda_pedido_area_orden
    ON operaciones.comandas (pedido_codigo, area_id) WHERE tipo = 'ORDEN';
CREATE UNIQUE INDEX uq_comanda_anulacion
    ON operaciones.comandas (anulacion_id) WHERE anulacion_id IS NOT NULL;
CREATE INDEX idx_comandas_estado ON operaciones.comandas (estado);
CREATE INDEX idx_comandas_pedido ON operaciones.comandas (pedido_codigo);

CREATE TABLE operaciones.comanda_lineas (
    id              BIGSERIAL PRIMARY KEY,
    comanda_id      BIGINT       NOT NULL REFERENCES operaciones.comandas (id) ON DELETE CASCADE,
    producto_id     BIGINT       NOT NULL,
    nombre_producto VARCHAR(120) NOT NULL,
    cantidad        INT          NOT NULL CHECK (cantidad > 0),
    extras          VARCHAR(300),
    ingredientes    VARCHAR(300),
    observaciones   VARCHAR(500),
    orden           INT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_comanda_lineas_comanda ON operaciones.comanda_lineas (comanda_id);
