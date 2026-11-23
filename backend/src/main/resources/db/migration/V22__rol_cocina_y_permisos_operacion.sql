-- =====================================================================
-- Rol COCINA y permisos de operación (comandas, entrega en salón y retiro).
--
-- Decisión del negocio: la cocina marca todos los estados de las comandas y
-- entrega los pedidos de retiro en el local; el mesero puede hacer las
-- mismas acciones. V10 había eliminado COCINA como rol (la cocina eran solo
-- permisos explícitos sobre MESERO); aquí se recrea de forma deliberada.
--
--   1. Crea el rol COCINA (categoría EMPLEADO, asignable y configurable
--      desde el panel) con sus permisos por defecto.
--   2. Concede al rol MESERO los permisos de operación que no tenía.
--
-- Los usuarios con permisos explícitos (p. ej. los cocineros migrados por
-- V10) NO cambian: el administrador puede reasignarlos a COCINA. Los
-- permisos nuevos de un rol llegan a una sesión abierta cuando renueva su
-- token o vuelve a iniciar sesión.
--
-- Idempotente: puede reaplicarse sin duplicar nada.
-- =====================================================================

-- 1. El rol COCINA
INSERT INTO seguridad.roles (codigo, descripcion, categoria_id, asignable_por_admin, configurable, origen)
SELECT 'COCINA',
       'Cocina: prepara las comandas y entrega los pedidos de retiro',
       c.id, TRUE, TRUE, 'ADMIN'
FROM seguridad.categorias_roles c
WHERE c.codigo = 'EMPLEADO'
  AND NOT EXISTS (SELECT 1 FROM seguridad.roles WHERE codigo = 'COCINA');

SELECT setval('seguridad.roles_id_seq', (SELECT MAX(id) FROM seguridad.roles));

-- Permisos por defecto de COCINA: ver comandas, marcar preparación/listo,
-- reimprimir, consultar áreas y entregar retiros en el mostrador.
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo IN (
    'catalogo:ver',
    'comandas:ver',
    'comandas:reenviar',
    'pedidos:estado-preparacion',
    'pedidos:estado-listo',
    'pedidos:retiro-mostrador')
WHERE r.codigo = 'COCINA'
ON CONFLICT DO NOTHING;

-- 2. MESERO: puede operar la cocina y entregar (salón y retiro).
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo IN (
    'pedidos:estado-preparacion',
    'pedidos:entregar',
    'pedidos:retiro-mostrador',
    'comandas:reenviar')
WHERE r.codigo = 'MESERO'
ON CONFLICT DO NOTHING;
