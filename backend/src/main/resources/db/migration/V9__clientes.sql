-- =====================================================================
-- V9__clientes.sql  Fase 7: Pedidos del cliente RF-40 a RF-45. ADITIVA.
-- Todo esto es ADITIVO sobre V1..V8: NO edita migraciones previas (checksum
-- Flyway). Anexo el patron de la fase: insert aditivo por fase con ids
-- crecientes y numeracion que continua desde la anterior.

-- 1. Rol CLIENTE id 5 + permisos ids CONTINUAN la secuencia V8 (V1..V8 usan
--    ids 1..31). Los permisos de clientes arrancan en 32.
INSERT INTO roles (id, codigo, descripcion) VALUES
(5, 'CLIENTE', 'Cliente de la app movil con QR (RF-40 a RF-45)');

INSERT INTO permisos (id, codigo, modulo, descripcion) VALUES
(32, 'clientes:menu-ver',           'clientes', 'Ver menu publico desde la app'),
(33, 'clientes:carrito-gestionar',  'clientes', 'Gestionar carrito de compra'),
(34, 'clientes:pedido-crear',       'clientes', 'Crear pedido desde la app'),
(35, 'clientes:pedido-confirmar',   'clientes', 'Confirmar pedido con idempotencia'),
(36, 'clientes:pedido-estado-ver',  'clientes', 'Ver estado del pedido en tiempo real'),
(37, 'clientes:historial-ver',      'clientes', 'Ver historial de pedidos'),
(38, 'clientes:cuenta-nueva',       'clientes', 'Registrar/recuperar cuenta de cliente');

INSERT INTO roles_permisos (rol_id, permiso_id) VALUES
(5, 32), (5, 33), (5, 34), (5, 35), (5, 36), (5, 37), (5, 38);

-- 2. Perfil del cliente: ligado al usuario logueado (RF-45 historial). RF-49
--    la app "abre" por QR primero, pero RF-45 exige el usuario real que se
--    loguea (decision del usuario: login real primero, RF-44 tablet).
CREATE TABLE clientes (
    id              BIGSERIAL PRIMARY KEY,
    usuario_id      BIGINT      NOT NULL UNIQUE REFERENCES usuarios (id),
    cedula          VARCHAR(20) NOT NULL UNIQUE,
    telefono        VARCHAR(20) NOT NULL UNIQUE,
    nombre          VARCHAR(80) NOT NULL,
    version         BIGINT      NOT NULL DEFAULT 0,
    creado_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Direcciones del cliente para domicilio (RF-42). RF-45 historial por
--    usuario, RF-41 metodo de entrega congelado.
CREATE TABLE clientes_direcciones (
    id            BIGSERIAL PRIMARY KEY,
    cliente_id    BIGINT       NOT NULL REFERENCES clientes (id),
    etiqueta      VARCHAR(40)  NOT NULL,
    direccion     VARCHAR(200) NOT NULL,
    telefono      VARCHAR(20),
    observaciones VARCHAR(200),
    activa        BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 4. Pedido del cliente. RF-40 carrito -> RF-41 confirmar idempotente (UNIQUE
--    idempotency_key por cliente = reintento 200, clave distinta 409), RF-43
--    push estado por WebSocket (estado en tiempo real), RF-44 tablet marca
--    EN_PREPARACION/LISTO, RF-45 historial por cliente. Transiciones en UNA
--    clase (RF-44 RF-15..16 reuso): BORRADOR -> CONFIRMADO -> EN_PREPARACION
--    -> LISTO -> ENTREGADO (o ANULADO).
CREATE TABLE pedidos_clientes (
    id               BIGSERIAL PRIMARY KEY,
    codigo           VARCHAR(40)  NOT NULL UNIQUE,
    cliente_id       BIGINT       NOT NULL REFERENCES clientes (id),
    metodo_pago      VARCHAR(20)  NOT NULL CHECK (metodo_pago IN ('EFECTIVO','TARJETA','TRANSFERENCIA')),
    metodo_entrega   VARCHAR(20)  NOT NULL CHECK (metodo_entrega IN ('RETIRAR','DOMICILIO')),
    direccion_id     BIGINT       REFERENCES clientes_direcciones (id),
    estado           VARCHAR(20)  NOT NULL CHECK (estado IN ('BORRADOR','CONFIRMADO','EN_PREPARACION','LISTO','ENTREGADO','ANULADO')),
    idempotency_key  VARCHAR(100) NOT NULL,
    version          BIGINT       NOT NULL DEFAULT 0,
    confirmado_at    TIMESTAMPTZ,
    creado_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actualizado_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_pedido_cliente_idempotencia UNIQUE (cliente_id, idempotency_key)
);

-- 5. Lineas congeladas con precio RF-41 (Money con regla unica RF-09): al
--    confirmar se congelan nombre + precio + extras; luego el catalogo no
--    cambia la linea (RF-41 "sobre cada linea"). RF-40 carrito en memoria
--    reusa esta estructura.
CREATE TABLE pedidos_clientes_lineas (
    id            BIGSERIAL PRIMARY KEY,
    pedido_id     BIGINT        NOT NULL REFERENCES pedidos_clientes (id),
    producto_id   BIGINT        NOT NULL REFERENCES productos (id),
    nombre        VARCHAR(100)  NOT NULL,
    precio        NUMERIC(12,2) NOT NULL,
    cantidad      INT           NOT NULL CHECK (cantidad > 0),
    observaciones VARCHAR(200),
    creado_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE pedidos_clientes_lineas_extras (
    id       BIGSERIAL PRIMARY KEY,
    linea_id BIGINT        NOT NULL REFERENCES pedidos_clientes_lineas (id),
    extra_id BIGINT        NOT NULL REFERENCES extras (id),
    nombre   VARCHAR(100)  NOT NULL,
    precio   NUMERIC(12,2) NOT NULL,
    creado_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 6. Outbox para push RF-43 al cliente (WebSocket/STOMP): la tabla outbox de
--    V4 se reutiliza tal cual (modulo comandas), aqui el agregado es el
--    pedido del cliente y el evento 'pedido-cliente-estado' RF-43/RF-44.
--    No se crea tabla nueva; el listener del modulo clientes escribe a la
--    MISMA outbox V4 con agregado_id = codigo del pedido del cliente.
CREATE INDEX idx_pedcli_cliente ON pedidos_clientes (cliente_id);
CREATE INDEX idx_pedcli_estado  ON pedidos_clientes (estado);
