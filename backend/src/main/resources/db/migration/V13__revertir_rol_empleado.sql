-- =====================================================================
-- Revertir la fusión MESERO/COCINA → EMPLEADO.
--
-- V12__roles_empleados_delivery.sql (ya aplicada en las bases desplegadas
-- desde main) creaba el rol EMPLEADO, movía a los usuarios MESERO/COCINA
-- a EMPLEADO y copiaba los permisos de sus roles antiguos como permisos
-- explícitos del usuario. Ese diseño se descartó: los roles vuelven a ser
-- ADMIN | MESERO | COCINA | DELIVERY | CLIENTE.
--
-- Esta migración es idempotente y ADITIVA (no edita V12, que ya está
-- aplicada y no puede recalcularse):
--   1. devuelve cada usuario EMPLEADO a su rol original (MESERO si tenía
--      permisos de mesero, COCINA si tenía permisos de cocina) y le borra
--      los permisos explícitos heredados para que rijan los defaults del
--      rol restaurado;
--   2. elimina el rol EMPLEADO y sus asignaciones.
-- En una base recién creada (donde V8__seed ya crea MESERO/COCINA) no hay
-- nada que revertir y esta migración no hace nada.
-- =====================================================================

-- ---------------------------------------------------------------
-- 1. Usuarios EMPLEADO → MESERO o COCINA según sus permisos explícitos
--    (los que V12 copió del rol antiguo).
-- ---------------------------------------------------------------
CREATE TEMP TABLE revertir_empleado ON COMMIT DROP AS
SELECT u.id AS usuario_id,
       CASE
           WHEN EXISTS (
               SELECT 1 FROM seguridad.roles_permisos rp
               JOIN seguridad.permisos p ON p.id = rp.permiso_id
               JOIN seguridad.roles r ON r.id = rp.rol_id
               WHERE r.codigo = 'COCINA'
                 AND rp.permiso_id IN (SELECT permiso_id FROM seguridad.usuarios_permisos up
                                       WHERE up.usuario_id = u.id)
                 AND rp.permiso_id NOT IN (SELECT permiso_id FROM seguridad.roles_permisos rp2
                                           JOIN seguridad.roles r2 ON r2.id = rp2.rol_id
                                           WHERE r2.codigo = 'MESERO')
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
-- 2. Baja del rol EMPLEADO
-- ---------------------------------------------------------------
DELETE FROM seguridad.roles_permisos
WHERE rol_id = (SELECT id FROM seguridad.roles WHERE codigo = 'EMPLEADO');

DELETE FROM seguridad.roles WHERE codigo = 'EMPLEADO';

SELECT setval('seguridad.roles_id_seq', (SELECT MAX(id) FROM seguridad.roles));
