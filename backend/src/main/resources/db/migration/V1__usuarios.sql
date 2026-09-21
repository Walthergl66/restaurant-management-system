-- V1: Usuarios, roles, permisos y tokens de refresco
-- Los roles y permisos se siembran aquí para que sean deterministas en todo entorno.

CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(30)  NOT NULL UNIQUE,
    descripcion VARCHAR(120),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE permisos (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(60)  NOT NULL UNIQUE,
    modulo      VARCHAR(30)  NOT NULL,
    descripcion VARCHAR(120),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE roles_permisos (
    rol_id     BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permiso_id BIGINT NOT NULL REFERENCES permisos (id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE usuarios (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    nombre        VARCHAR(100) NOT NULL,
    activo        BOOLEAN      NOT NULL DEFAULT TRUE,
    rol_id        BIGINT       NOT NULL REFERENCES roles (id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(50),
    updated_by    VARCHAR(50)
);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT      NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
token_hash VARCHAR(64)    NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_usuarios_rol ON usuarios (rol_id);
CREATE INDEX idx_refresh_tokens_usuario ON refresh_tokens (usuario_id);

-- ---------------------------------------------------------------
-- Seed de permisos (codigo: <modulo>:<accion>)
-- ---------------------------------------------------------------
INSERT INTO permisos (id, codigo, modulo, descripcion) VALUES
 -- usuarios
 (1,  'usuarios:ver',       'usuarios', 'Consultar usuarios'),
 (2,  'usuarios:crear',     'usuarios', 'Crear usuarios'),
 (3,  'usuarios:editar',    'usuarios', 'Editar usuarios'),
 (4,  'usuarios:eliminar',  'usuarios', 'Desactivar usuarios'),
 (5,  'roles:asignar',      'usuarios', 'Asignar roles a usuarios'),
 -- catalogo
 (6,  'catalogo:ver',       'catalogo', 'Ver catálogo (productos, extras, áreas)'),
 (7,  'catalogo:gestionar', 'catalogo', 'Administrar catálogo'),
 -- mesas
 (8,  'mesas:ver',          'mesas',    'Ver mesas y su estado'),
 (9,  'mesas:gestionar',    'mesas',    'Administrar mesas'),
 -- pedidos
 (10, 'pedidos:ver',        'pedidos',  'Ver pedidos'),
 (11, 'pedidos:crear',      'pedidos',  'Crear borradores de pedido'),
 (12, 'pedidos:editar',     'pedidos',  'Editar borradores de pedido'),
 (13, 'pedidos:confirmar',  'pedidos',  'Confirmar pedidos'),
 (14, 'pedidos:estado-preparacion', 'pedidos', 'Marcar pedido en preparación'),
 (15, 'pedidos:estado-listo',        'pedidos', 'Marcar pedido listo/entregado'),
 -- comandas
 (16, 'comandas:ver',       'comandas', 'Ver comandas'),
 (17, 'comandas:reenviar',  'comandas', 'Reenviar comandas'),
 -- anulaciones
 (18, 'anulaciones:solicitar', 'anulaciones', 'Solicitar anulación de línea'),
 (19, 'anulaciones:aprobar',   'anulaciones', 'Aprobar o rechazar anulaciones'),
 -- cuentas
 (20, 'cuentas:ver',        'cuentas',  'Ver cuentas de mesa'),
 (21, 'cuentas:gestionar',  'cuentas',  'Administrar cuentas'),
 -- pagos
 (22, 'pagos:cobrar',       'pagos',    'Registrar cobros'),
 (23, 'facturacion:emitir', 'facturacion', 'Emitir comprobantes'),
 -- caja
 (24, 'caja:apertura',      'caja',     'Abrir caja'),
 (25, 'caja:movimientos',   'caja',     'Registrar movimientos de caja'),
 (26, 'caja:cierre',        'caja',     'Cerrar caja'),
 -- finanzas / reportes / auditoría / clientes / configuración
 (27, 'finanzas:ver',       'finanzas', 'Ver finanzas'),
 (28, 'reportes:ver',       'reportes', 'Generar reportes'),
 (29, 'auditoria:ver',      'auditoria', 'Consultar auditoría'),
 (30, 'clientes:gestionar', 'clientes', 'Administrar clientes'),
 (31, 'configuracion:gestionar', 'configuracion', 'Administrar configuración');

-- ---------------------------------------------------------------
-- Seed de roles
-- ---------------------------------------------------------------
INSERT INTO roles (id, codigo, descripcion) VALUES
 (1, 'ADMIN',  'Administrador del sistema'),
 (2, 'MESERO', 'Mesero con acceso al flujo de pedidos'),
 (3, 'COCINA', 'Personal de cocina'),
 (4, 'CAJERO', 'Cajero que cobra y maneja caja');

-- ADMIN: todos los permisos
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT 1, id FROM permisos;

-- MESERO: nada de cobro/caja/finanzas internas, puede gestionar clientes y pedidos
INSERT INTO roles_permisos (rol_id, permiso_id) VALUES
 (2, 6), (2, 8), (2, 10), (2, 11), (2, 12), (2, 13), (2, 15),
 (2, 16), (2, 18), (2, 20), (2, 21), (2, 30);

-- COCINA: catálogo visible y estados de preparación
INSERT INTO roles_permisos (rol_id, permiso_id) VALUES
 (3, 6), (3, 10), (3, 14), (3, 15), (3, 16), (3, 17), (3, 18);

-- CAJERO: cobro, caja, facturación y lectura de lo necesario
INSERT INTO roles_permisos (rol_id, permiso_id) VALUES
 (4, 6), (4, 8), (4, 10), (4, 16), (4, 18), (4, 19), (4, 20),
 (4, 22), (4, 23), (4, 24), (4, 25), (4, 26), (4, 27), (4, 28);