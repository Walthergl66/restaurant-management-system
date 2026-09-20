-- V2: Catálogo (categorías, áreas, productos, extras, ingredientes removibles),
-- mesas, impresoras y parámetros de configuración del restaurante.
-- Todos los precios son NUMERIC(12,2): el dinero se guarda siempre con BigDecimal.

-- ---------------------------------------------------------------
-- Catálogo
-- ---------------------------------------------------------------
CREATE TABLE categorias (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(80)  NOT NULL UNIQUE,
    descripcion VARCHAR(200),
    orden       INT          NOT NULL DEFAULT 0,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

-- Área de preparación: divide las comandas (cocina, barra, postres...).
CREATE TABLE areas (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(80)  NOT NULL UNIQUE,
    descripcion VARCHAR(200),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

-- Adición que puede acompañar un producto (topping).
CREATE TABLE extras (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(80)  NOT NULL UNIQUE,
    descripcion VARCHAR(200),
    precio      NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (precio >= 0),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

CREATE TABLE productos (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(120) NOT NULL UNIQUE,
    descripcion  VARCHAR(500),
    imagen_url   VARCHAR(300),
    precio       NUMERIC(12,2) NOT NULL CHECK (precio >= 0),
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    categoria_id BIGINT       REFERENCES categorias (id),
    area_id      BIGINT       REFERENCES areas (id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   VARCHAR(50),
    updated_by   VARCHAR(50)
);

-- Asociación producto ↔ extra (una adición puede pertenecer a varios productos).
CREATE TABLE producto_extras (
    producto_id BIGINT NOT NULL REFERENCES productos (id) ON DELETE CASCADE,
    extra_id    BIGINT NOT NULL REFERENCES extras (id) ON DELETE CASCADE,
    PRIMARY KEY (producto_id, extra_id)
);

-- Ingredientes que el cliente puede pedir remover (sin cebolla, sin tomate...).
CREATE TABLE producto_ingredientes (
    id          BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES productos (id) ON DELETE CASCADE,
    nombre      VARCHAR(80) NOT NULL,
    activo      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

CREATE INDEX idx_productos_categoria ON productos (categoria_id);
CREATE INDEX idx_productos_area ON productos (area_id);
CREATE INDEX idx_producto_ingredientes_producto ON producto_ingredientes (producto_id);

-- ---------------------------------------------------------------
-- Mesas
-- ---------------------------------------------------------------
CREATE TABLE mesas (
    id         BIGSERIAL PRIMARY KEY,
    numero     INT NOT NULL UNIQUE,
    capacidad  INT NOT NULL CHECK (capacidad BETWEEN 1 AND 50),
    ubicacion  VARCHAR(100) NOT NULL DEFAULT 'SALON',
    estado     VARCHAR(20)  NOT NULL DEFAULT 'LIBRE'
               CHECK (estado IN ('LIBRE', 'OCUPADA', 'RESERVADA', 'INACTIVA')),
    activo     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

-- ---------------------------------------------------------------
-- Configuración del restaurante
-- ---------------------------------------------------------------
-- Parámetros genéricos clave/valor.
CREATE TABLE parametros (
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

-- Impresoras térmicas: destino de las órdenes de impresión (Fase 4).
CREATE TABLE impresoras (
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

-- ---------------------------------------------------------------
-- Seed: parámetros por defecto del restaurante
-- ---------------------------------------------------------------
INSERT INTO parametros (clave, valor, tipo, descripcion) VALUES
 ('restaurant.nombre', 'Restaurante',  'TEXTO',    'Nombre del restaurante que aparece en el menú público'),
 ('restaurant.direccion', '',          'TEXTO',    'Dirección del restaurante'),
 ('restaurant.telefono', '',           'TEXTO',    'Teléfono del restaurante'),
 ('impuestos.iva', '0.15',             'NUMERICO', 'IVA en tanto por uno, incluido en los precios de catálogo'),
 ('moneda', 'USD',                     'TEXTO',    'Código de moneda para precios y comprobantes');