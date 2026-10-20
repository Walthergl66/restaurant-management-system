-- =====================================================================
-- Reorganización de roles: ADMIN | EMPLEADO | DELIVERY | CLIENTE.
--
--  - MESERO y COCINA se fusionan en EMPLEADO. Cada usuario de esos roles
--    pasa a EMPLEADO y sus permisos anteriores quedan como permisos
--    EXPLÍCITOS del usuario (usuarios_permisos), de modo que su acceso
--    no cambia y el administrador puede ajustarlos desde el panel.
--  - DELIVERY es un rol nuevo para repartidores.
--  - Nuevos permisos de entrega para el flujo de delivery.
--  - La tabla seguridad.usuarios_permisos guarda los permisos
--    explícitos por usuario: si un usuario no tiene filas, rigen los
--    permisos por defecto de su rol.
-- =====================================================================

-- ---------------------------------------------------------------
-- Nuevos permisos del flujo de entrega
-- ---------------------------------------------------------------
INSERT INTO seguridad.permisos (id, codigo, modulo, descripcion) VALUES
 (39, 'pedidos:entregar',        'pedidos', 'Marcar pedido como entregado'),
 (40, 'pedidos:retiro-mostrador', 'pedidos', 'Confirmar retiro de pedido en mostrador');

SELECT setval('seguridad.permisos_id_seq', (SELECT MAX(id) FROM seguridad.permisos));

-- ---------------------------------------------------------------
-- Permisos explícitos por usuario
-- ---------------------------------------------------------------
CREATE TABLE seguridad.usuarios_permisos (
    usuario_id BIGINT NOT NULL REFERENCES seguridad.usuarios (id) ON DELETE CASCADE,
    permiso_id BIGINT NOT NULL REFERENCES seguridad.permisos (id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, permiso_id)
);

CREATE INDEX idx_usuarios_permisos_usuario ON seguridad.usuarios_permisos (usuario_id);

-- ---------------------------------------------------------------
-- Roles nuevos
-- ---------------------------------------------------------------
INSERT INTO seguridad.roles (id, codigo, descripcion) VALUES
 (6, 'EMPLEADO', 'Personal interno: mesero, cocina o caja según sus permisos'),
 (7, 'DELIVERY', 'Repartidor: entrega pedidos y confirma retiros');

-- ---------------------------------------------------------------
-- Permisos por defecto de los nuevos roles (base que el panel
-- pre-marca y que el administrador puede ajustar por usuario)
-- ---------------------------------------------------------------
-- EMPLEADO: unión de los defaults de los antiguos MESERO y COCINA
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo IN (
    'catalogo:ver', 'mesas:ver',
    'pedidos:ver', 'pedidos:crear', 'pedidos:editar', 'pedidos:confirmar',
    'pedidos:estado-preparacion', 'pedidos:estado-listo',
    'comandas:ver', 'comandas:reenviar',
    'anulaciones:solicitar', 'cuentas:ver', 'cuentas:gestionar',
    'clientes:gestionar')
WHERE r.codigo = 'EMPLEADO';

-- DELIVERY: ver/fluir pedidos de entrega y consultar catálogo y mesas
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo IN (
    'catalogo:ver', 'mesas:ver',
    'pedidos:ver', 'pedidos:entregar', 'pedidos:retiro-mostrador')
WHERE r.codigo = 'DELIVERY';

-- ---------------------------------------------------------------
-- Migración de usuarios existentes: MESERO/COCINA -> EMPLEADO
-- Copia los permisos del rol antiguo como permisos explícitos del
-- usuario para conservar exactamente su acceso actual.
-- ---------------------------------------------------------------
CREATE TEMP TABLE roles_antiguos AS
SELECT id, codigo FROM seguridad.roles WHERE codigo IN ('MESERO', 'COCINA');

INSERT INTO seguridad.usuarios_permisos (usuario_id, permiso_id)
SELECT u.id, rp.permiso_id
FROM seguridad.usuarios u
JOIN roles_antiguos ra ON ra.id = u.rol_id
JOIN seguridad.roles_permisos rp ON rp.rol_id = ra.id;

UPDATE seguridad.usuarios u
SET rol_id = (SELECT id FROM seguridad.roles WHERE codigo = 'EMPLEADO')
FROM roles_antiguos ra
WHERE ra.id = u.rol_id;

DROP TABLE roles_antiguos;

-- ---------------------------------------------------------------
-- Baja de los roles fusionados
-- ---------------------------------------------------------------
DELETE FROM seguridad.roles_permisos
WHERE rol_id IN (SELECT id FROM seguridad.roles WHERE codigo IN ('MESERO', 'COCINA'));

DELETE FROM seguridad.roles WHERE codigo IN ('MESERO', 'COCINA');

SELECT setval('seguridad.roles_id_seq', (SELECT MAX(id) FROM seguridad.roles));
