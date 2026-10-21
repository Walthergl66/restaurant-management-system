-- =====================================================================
-- Restaurar los roles MESERO y COCINA (deshace la fusión en EMPLEADO).
--
-- V12__roles_empleados_delivery.sql (ya aplicada en las bases desplegadas
-- desde main) hacía tres cosas que acá se revierten:
--   1. creaba el rol EMPLEADO (id 6);
--   2. BORRABA los roles MESERO y COCINA con sus asignaciones;
--   3. movía a sus usuarios a EMPLEADO copiando los permisos del rol
--      antiguo como permisos explícitos del usuario.
--
-- Los roles vuelven a ser ADMIN | MESERO | COCINA | DELIVERY | CLIENTE.
-- Esta migración es idempotente y aditiva (no edita V12, que ya está
-- aplicada y no puede recalcularse):
--   1. recrea MESERO y COCINA con sus permisos por defecto (los mismos
--      que define V8__seed.sql);
--   2. devuelve cada usuario EMPLEADO a su rol original según los
--      permisos que V12 copió (COCINA si tenía los de cocina, si no
--      MESERO) y le borra esos permisos explícitos heredados para que
--      rijan los defaults del rol restaurado;
--   3. elimina el rol EMPLEADO y sus asignaciones.
-- =====================================================================

-- ---------------------------------------------------------------
-- 1. Recrear MESERO y COCINA con sus permisos por defecto
-- ---------------------------------------------------------------
INSERT INTO seguridad.roles (codigo, descripcion)
SELECT 'MESERO', 'Mesero con acceso al flujo de pedidos'
WHERE NOT EXISTS (SELECT 1 FROM seguridad.roles WHERE codigo = 'MESERO');

INSERT INTO seguridad.roles (codigo, descripcion)
SELECT 'COCINA', 'Personal de cocina'
WHERE NOT EXISTS (SELECT 1 FROM seguridad.roles WHERE codigo = 'COCINA');

-- Permisos por defecto de MESERO (los mismos de V8__seed.sql)
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo IN (
    'catalogo:ver', 'mesas:ver',
    'pedidos:ver', 'pedidos:crear', 'pedidos:editar', 'pedidos:confirmar',
    'pedidos:estado-listo',
    'comandas:ver',
    'anulaciones:solicitar',
    'cuentas:ver', 'cuentas:gestionar',
    'clientes:gestionar')
WHERE r.codigo = 'MESERO'
ON CONFLICT DO NOTHING;

-- Permisos por defecto de COCINA (los mismos de V8__seed.sql)
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo IN (
    'catalogo:ver',
    'pedidos:ver', 'pedidos:estado-preparacion', 'pedidos:estado-listo',
    'comandas:ver', 'comandas:reenviar',
    'anulaciones:solicitar')
WHERE r.codigo = 'COCINA'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------
-- 2. Usuarios EMPLEADO → COCINA o MESERO según los permisos que V12
--    copió del rol antiguo (cocina se distingue por sus permisos
--    exclusivos, p. ej. pedidos:estado-preparacion / comandas:reenviar).
-- ---------------------------------------------------------------
CREATE TEMP TABLE revertir_empleado ON COMMIT DROP AS
SELECT u.id AS usuario_id,
       CASE
           WHEN EXISTS (
               SELECT 1
               FROM seguridad.usuarios_permisos up
               JOIN seguridad.permisos p ON p.id = up.permiso_id
               WHERE up.usuario_id = u.id
                 AND p.codigo IN ('pedidos:estado-preparacion', 'comandas:reenviar')
           ) THEN 'COCINA'
           ELSE 'MESERO'
       END AS rol_codigo
FROM seguridad.usuarios u
JOIN seguridad.roles re ON re.id = u.rol_id AND re.codigo = 'EMPLEADO';

UPDATE seguridad.usuarios u
SET rol_id = (SELECT r.id FROM seguridad.roles r WHERE r.codigo = rev.rol_codigo)
FROM revertir_empleado rev
WHERE rev.usuario_id = u.id;

-- Los permisos explícitos heredados de V12 dejarían "congelado" el acceso
-- (getPermisoCodigos() los prefiere sobre los defaults del rol): se borran
-- para que el usuario vuelva a heredar los defaults de MESERO/COCINA.
DELETE FROM seguridad.usuarios_permisos up
USING revertir_empleado rev
WHERE up.usuario_id = rev.usuario_id;

-- ---------------------------------------------------------------
-- 3. Baja del rol EMPLEADO
-- ---------------------------------------------------------------
DELETE FROM seguridad.roles_permisos
WHERE rol_id = (SELECT id FROM seguridad.roles WHERE codigo = 'EMPLEADO');

DELETE FROM seguridad.roles WHERE codigo = 'EMPLEADO';

SELECT setval('seguridad.roles_id_seq', (SELECT MAX(id) FROM seguridad.roles));
