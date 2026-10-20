-- =====================================================================
-- Rol DELIVERY y permisos explícitos por usuario.
--
--  - Roles vigentes: ADMIN | MESERO | COCINA | DELIVERY | CLIENTE
--    (V13 deshace la fusión MESERO/COCINA que hizo
--    V12__roles_empleados_delivery.sql).
--  - DELIVERY es un rol nuevo para repartidores, con permisos acotados
--    al flujo de entrega.
--  - La tabla seguridad.usuarios_permisos guarda los permisos
--    explícitos por usuario: si un usuario no tiene filas, rigen los
--    permisos por defecto de su rol. Solo admite permisos explícitos
--    los roles configurables (MESERO, COCINA, DELIVERY); ADMIN y
--    CLIENTE quedan fijos.
-- =====================================================================

-- ---------------------------------------------------------------
-- Permisos del flujo de entrega
-- ---------------------------------------------------------------
INSERT INTO seguridad.permisos (codigo, modulo, descripcion) VALUES
 ('pedidos:entregar',         'pedidos', 'Marcar pedido como entregado'),
 ('pedidos:retiro-mostrador', 'pedidos', 'Confirmar retiro de pedido en mostrador')
ON CONFLICT (codigo) DO NOTHING;

-- ---------------------------------------------------------------
-- Permisos explícitos por usuario
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS seguridad.usuarios_permisos (
    usuario_id BIGINT NOT NULL REFERENCES seguridad.usuarios (id) ON DELETE CASCADE,
    permiso_id BIGINT NOT NULL REFERENCES seguridad.permisos (id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, permiso_id)
);

CREATE INDEX IF NOT EXISTS idx_usuarios_permisos_usuario
    ON seguridad.usuarios_permisos (usuario_id);

-- ---------------------------------------------------------------
-- Rol DELIVERY
-- ---------------------------------------------------------------
INSERT INTO seguridad.roles (codigo, descripcion)
SELECT 'DELIVERY', 'Repartidor: entrega pedidos y confirma retiros'
WHERE NOT EXISTS (SELECT 1 FROM seguridad.roles WHERE codigo = 'DELIVERY');

-- ---------------------------------------------------------------
-- Permisos por defecto de DELIVERY: ver/fluir pedidos de entrega
-- y consultar catálogo y mesas.
-- ---------------------------------------------------------------
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo IN (
    'catalogo:ver', 'mesas:ver',
    'pedidos:ver', 'pedidos:entregar', 'pedidos:retiro-mostrador')
WHERE r.codigo = 'DELIVERY'
ON CONFLICT DO NOTHING;
