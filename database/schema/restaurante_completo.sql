-- =====================================================================
--  SISTEMA DE GESTIÓN DE PEDIDOS DE RESTAURANTE - MODELO DE DATOS
--  Archivo unificado: todos los schemas y todas las tablas del proyecto.
--
--  PostgreSQL | 7 schemas | 34 tablas | 2 secuencias
--
--  Schemas y responsabilidad:
--    01 seguridad     Identidad y autorización (usuarios, roles, permisos, sesiones)
--    02 catalogo      Catálogo y recursos (productos, extras, áreas, mesas)
--    03 configuracion Parámetros KV y dispositivos (impresoras)
--    04 operaciones   Ciclo del servicio en sala (pedidos, comandas, cuentas, anulaciones)
--    05 comercial     Canal del cliente final (clientes, direcciones, pedidos app)
--    06 tesoreria     Dinero (cajas, pagos, movimientos, comprobantes)
--    07 auditoria     Trazabilidad y entrega fiable (auditoría, outbox)
--    08 seed          Datos iniciales (opcional)
--
--  Ejecutar en orden (las FK cruzan schemas):
--    01_seguridad -> 02_catalogo -> 03_configuracion -> 04_operaciones
--      -> 05_comercial -> 06_tesoreria -> 07_auditoria -> 08_seed
--  Este archivo unificado ya respeta ese orden.
-- =====================================================================

-- =====================================================================
-- Schema: SEGURIDAD
-- Responsabilidad: identidad y autorización de los usuarios del sistema.
-- Aquí vive quién es cada usuario (usuarios, refresh_tokens para sesiones)
-- y qué puede hacer (roles, permisos, roles_permisos). Se aísla porque es
-- el perímetro de acceso: cualquier falla o consulta errante en este schema
-- afecta la autenticación de TODO el sistema, no solo un módulo de negocio.
--
-- Tablas: roles, permisos, roles_permisos, usuarios, refresh_tokens.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS seguridad;

CREATE TABLE seguridad.roles (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(30)  NOT NULL UNIQUE,
    descripcion VARCHAR(120),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE seguridad.permisos (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(60)  NOT NULL UNIQUE,
    modulo      VARCHAR(30)  NOT NULL,
    descripcion VARCHAR(120),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE seguridad.roles_permisos (
    rol_id     BIGINT NOT NULL REFERENCES seguridad.roles (id) ON DELETE CASCADE,
    permiso_id BIGINT NOT NULL REFERENCES seguridad.permisos (id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE seguridad.usuarios (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    rol_id          BIGINT       NOT NULL REFERENCES seguridad.roles (id),
    sesion_version  BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50)
);

CREATE TABLE seguridad.refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT      NOT NULL REFERENCES seguridad.usuarios (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_usuarios_rol ON seguridad.usuarios (rol_id);
CREATE INDEX idx_refresh_tokens_usuario ON seguridad.refresh_tokens (usuario_id);

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

-- =====================================================================
-- Archivo 08 (OPCIONAL): datos iniciales del sistema.
-- Semillas de V1, V2, V6 y V9 para que la base de datos nazca funcional:
-- permisos, roles, asignaciones rol↔permiso y parámetros por defecto.
-- Todo apuntando al schema correspondiente.
-- =====================================================================

-- ---------------------------------------------------------------
-- Permisos (codigo: <modulo>:<accion>)
-- ---------------------------------------------------------------
INSERT INTO seguridad.permisos (id, codigo, modulo, descripcion) VALUES
 (1,  'usuarios:ver',       'usuarios', 'Consultar usuarios'),
 (2,  'usuarios:crear',     'usuarios', 'Crear usuarios'),
 (3,  'usuarios:editar',    'usuarios', 'Editar usuarios'),
 (4,  'usuarios:eliminar',  'usuarios', 'Desactivar usuarios'),
 (5,  'roles:asignar',      'usuarios', 'Asignar roles a usuarios'),
 (6,  'catalogo:ver',       'catalogo', 'Ver catálogo (productos, extras, áreas)'),
 (7,  'catalogo:gestionar', 'catalogo', 'Administrar catálogo'),
 (8,  'mesas:ver',          'mesas',    'Ver mesas y su estado'),
 (9,  'mesas:gestionar',    'mesas',    'Administrar mesas'),
 (10, 'pedidos:ver',        'pedidos',  'Ver pedidos'),
 (11, 'pedidos:crear',      'pedidos',  'Crear borradores de pedido'),
 (12, 'pedidos:editar',     'pedidos',  'Editar borradores de pedido'),
 (13, 'pedidos:confirmar',  'pedidos',  'Confirmar pedidos'),
 (14, 'pedidos:estado-preparacion', 'pedidos', 'Marcar pedido en preparación'),
 (15, 'pedidos:estado-listo',        'pedidos', 'Marcar pedido listo/entregado'),
 (16, 'comandas:ver',       'comandas', 'Ver comandas'),
 (17, 'comandas:reenviar',  'comandas', 'Reenviar comandas'),
 (18, 'anulaciones:solicitar', 'anulaciones', 'Solicitar anulación de línea'),
 (19, 'anulaciones:aprobar',   'anulaciones', 'Aprobar o rechazar anulaciones'),
 (20, 'cuentas:ver',        'cuentas',  'Ver cuentas de mesa'),
 (21, 'cuentas:gestionar',  'cuentas',  'Administrar cuentas'),
 (22, 'pagos:cobrar',       'pagos',    'Registrar cobros'),
 (23, 'facturacion:emitir', 'facturacion', 'Emitir comprobantes'),
 (24, 'caja:apertura',      'caja',     'Abrir caja'),
 (25, 'caja:movimientos',   'caja',     'Registrar movimientos de caja'),
 (26, 'caja:cierre',        'caja',     'Cerrar caja'),
 (27, 'finanzas:ver',       'finanzas', 'Ver finanzas'),
 (28, 'reportes:ver',       'reportes', 'Generar reportes'),
 (29, 'auditoria:ver',      'auditoria', 'Consultar auditoría'),
 (30, 'clientes:gestionar', 'clientes', 'Administrar clientes'),
 (31, 'configuracion:gestionar', 'configuracion', 'Administrar configuración'),
 (32, 'clientes:menu-ver',           'clientes', 'Ver menú público desde la app'),
 (33, 'clientes:carrito-gestionar',  'clientes', 'Gestionar carrito de compra'),
 (34, 'clientes:pedido-crear',       'clientes', 'Crear pedido desde la app'),
 (35, 'clientes:pedido-confirmar',   'clientes', 'Confirmar pedido con idempotencia'),
 (36, 'clientes:pedido-estado-ver',  'clientes', 'Ver estado del pedido en tiempo real'),
 (37, 'clientes:historial-ver',      'clientes', 'Ver historial de pedidos'),
 (38, 'clientes:cuenta-nueva',       'clientes', 'Registrar/recuperar cuenta de cliente');

-- ---------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------
INSERT INTO seguridad.roles (id, codigo, descripcion) VALUES
 (1, 'ADMIN',   'Administrador del sistema'),
 (2, 'MESERO',  'Mesero con acceso al flujo de pedidos'),
 (3, 'COCINA',  'Personal de cocina'),
 (4, 'CAJERO',  'Cajero que cobra y maneja caja'),
 (5, 'CLIENTE', 'Cliente de la app móvil con QR');

-- ---------------------------------------------------------------
-- Asignaciones rol ↔ permiso
-- ---------------------------------------------------------------
-- ADMIN: todos los permisos
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT 1, id FROM seguridad.permisos;

-- MESERO
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id) VALUES
 (2, 6), (2, 8), (2, 10), (2, 11), (2, 12), (2, 13), (2, 15),
 (2, 16), (2, 18), (2, 20), (2, 21), (2, 30);

-- COCINA
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id) VALUES
 (3, 6), (3, 10), (3, 14), (3, 15), (3, 16), (3, 17), (3, 18);

-- CAJERO (incluye cuentas:gestionar concedida en V6)
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id) VALUES
 (4, 6), (4, 8), (4, 10), (4, 16), (4, 18), (4, 19), (4, 20),
 (4, 21), (4, 22), (4, 23), (4, 24), (4, 25), (4, 26), (4, 27), (4, 28);

-- CLIENTE
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id) VALUES
 (5, 32), (5, 33), (5, 34), (5, 35), (5, 36), (5, 37), (5, 38);

-- Las inserciones usaron ids explícitos: se pone la secuencia al día.
SELECT setval('seguridad.roles_id_seq', (SELECT MAX(id) FROM seguridad.roles));
SELECT setval('seguridad.permisos_id_seq', (SELECT MAX(id) FROM seguridad.permisos));

-- ---------------------------------------------------------------
-- Parámetros por defecto del restaurante
-- ---------------------------------------------------------------
INSERT INTO configuracion.parametros (clave, valor, tipo, descripcion) VALUES
 ('restaurant.nombre', 'Restaurante',  'TEXTO',    'Nombre del restaurante que aparece en el menú público'),
 ('restaurant.direccion', '',          'TEXTO',    'Dirección del restaurante'),
 ('restaurant.telefono', '',           'TEXTO',    'Teléfono del restaurante'),
 ('impuestos.iva', '0.15',             'NUMERICO', 'IVA en tanto por uno, incluido en los precios de catálogo'),
 ('moneda', 'USD',                     'TEXTO',    'Código de moneda para precios y comprobantes');
