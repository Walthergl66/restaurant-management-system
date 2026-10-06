-- =====================================================================
-- Schema: CATALOGO
-- Responsabilidad: lo que el restaurante ofrece y dónde se sirve.
-- Catálogo inmutable-transaccionalmente (productos, precios, categorías,
-- extras, ingredientes removibles), áreas de preparación que dividen las
-- comandas y las mesas del salón. Es el schema más referenciado: pedidos,
-- comandas, anulaciones y la app del cliente apuntan aquí, por eso va
-- antes que `operaciones` y `comercial` en el orden de ejecución.
--
-- Tablas: categorias, areas, extras, productos, producto_extras,
--         producto_ingredientes, mesas.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS catalogo;

CREATE TABLE catalogo.categorias (
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
CREATE TABLE catalogo.areas (
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
CREATE TABLE catalogo.extras (
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

CREATE TABLE catalogo.productos (
    id           BIGSERIAL PRIMARY KEY,
    nombre       VARCHAR(120) NOT NULL UNIQUE,
    descripcion  VARCHAR(500),
    imagen_url   VARCHAR(300),
    precio       NUMERIC(12,2) NOT NULL CHECK (precio >= 0),
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    categoria_id BIGINT       REFERENCES catalogo.categorias (id),
    area_id      BIGINT       REFERENCES catalogo.areas (id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   VARCHAR(50),
    updated_by   VARCHAR(50)
);

-- Asociación producto ↔ extra (una adición puede pertenecer a varios productos).
CREATE TABLE catalogo.producto_extras (
    producto_id BIGINT NOT NULL REFERENCES catalogo.productos (id) ON DELETE CASCADE,
    extra_id    BIGINT NOT NULL REFERENCES catalogo.extras (id) ON DELETE CASCADE,
    PRIMARY KEY (producto_id, extra_id)
);

-- Ingredientes que el cliente puede pedir remover (sin cebolla, sin tomate...).
CREATE TABLE catalogo.producto_ingredientes (
    id          BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES catalogo.productos (id) ON DELETE CASCADE,
    nombre      VARCHAR(80) NOT NULL,
    activo      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

CREATE INDEX idx_productos_categoria ON catalogo.productos (categoria_id);
CREATE INDEX idx_productos_area ON catalogo.productos (area_id);
CREATE INDEX idx_producto_ingredientes_producto ON catalogo.producto_ingredientes (producto_id);

-- Mesas: recurso físico del salón con máquina de estados propia
-- (LIBRE/OCUPADA/RESERVADA/INACTIVA) y versión optimista.
CREATE TABLE catalogo.mesas (
    id         BIGSERIAL PRIMARY KEY,
    numero     INT NOT NULL UNIQUE,
    capacidad  INT NOT NULL CHECK (capacidad BETWEEN 1 AND 50),
    ubicacion  VARCHAR(100) NOT NULL DEFAULT 'SALON',
    estado     VARCHAR(20)  NOT NULL DEFAULT 'LIBRE'
               CHECK (estado IN ('LIBRE', 'OCUPADA', 'RESERVADA', 'INACTIVA')),
    activo     BOOLEAN NOT NULL DEFAULT TRUE,
    version    BIGINT  NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);
