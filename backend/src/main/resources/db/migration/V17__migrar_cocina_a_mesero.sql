-- =====================================================================
-- Los cocineros pasan a MESERO y el rol COCINA se da de baja.
--
--  - COCINA desaparece como rol: la cocina deja de ser un rol y pasa a
--    ser una funcionalidad (abrir comandas y marcar la preparación),
--    activable por el administrador sobre cualquier empleado.
--
--  - Los usuarios que eran COCINA pasan a MESERO, que es su subrol dentro
--    de la categoría EMPLEADO.
--
--  - Conservan el acceso a la cocina: como MESERO no trae los permisos
--    pedidos:estado-preparacion ni comandas:reenviar, se les guardan
--    como permisos EXPLÍCITOS.
--
--  - El set explícito es COMPLETO a propósito: si un usuario tiene
--    cualquier permiso explícito, rigen solo esos y no los del rol
--    (Usuario.getPermisoCodigos). Por eso no basta con añadir los dos
--    de cocina: habría que quitarle todo lo demás.
--
--  - Se incrementa sesion_version: el token en circulación lleva los
--    permisos de COCINO, así que el usuario tiene que volver a entrar.
--
--  - Migración aditiva e idempotente.
-- =====================================================================

-- ---------------------------------------------------------------
-- Usuarios con rol COCINA (se capturan antes de moverlos de rol)
-- ---------------------------------------------------------------
CREATE TEMP TABLE migrados_cocina ON COMMIT DROP AS
SELECT u.id
FROM seguridad.usuarios u
JOIN seguridad.roles r ON r.id = u.rol_id
WHERE r.codigo = 'COCINA';

-- ---------------------------------------------------------------
-- Permisos explícitos: los del subrol MESERO
-- ---------------------------------------------------------------
INSERT INTO seguridad.usuarios_permisos (usuario_id, permiso_id)
SELECT m.id, rp.permiso_id
FROM migrados_cocina m
JOIN seguridad.roles_permisos rp ON TRUE
JOIN seguridad.roles r ON r.id = rp.rol_id
WHERE r.codigo = 'MESERO'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------
-- Permisos explícitos: los de cocina que MESERO no trae
-- (los que el usuario tenía como COCINA)
-- ---------------------------------------------------------------
INSERT INTO seguridad.usuarios_permisos (usuario_id, permiso_id)
SELECT m.id, rp.permiso_id
FROM migrados_cocina m
JOIN seguridad.roles_permisos rp ON TRUE
JOIN seguridad.roles r ON r.id = rp.rol_id
WHERE r.codigo = 'COCINA'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------
-- Cambio de rol: COCINA -> MESERO
-- ---------------------------------------------------------------
UPDATE seguridad.usuarios u
SET rol_id    = (SELECT id FROM seguridad.roles WHERE codigo = 'MESERO'),
    updated_at = now(),
    updated_by = 'migracion-v17'
FROM migrados_cocina m
WHERE u.id = m.id;

-- ---------------------------------------------------------------
-- Las sesiones abiertas siguen llevando los permisos de COCINA en el
-- token: se invalida la versión de sesión para forzar el reingreso.
-- ---------------------------------------------------------------
UPDATE seguridad.usuarios u
SET sesion_version = u.sesion_version + 1
FROM migrados_cocina m
WHERE u.id = m.id;

-- ---------------------------------------------------------------
-- Baja del rol COCINA
-- ---------------------------------------------------------------
DELETE FROM seguridad.roles_permisos
WHERE rol_id = (SELECT id FROM seguridad.roles WHERE codigo = 'COCINA');

DELETE FROM seguridad.roles WHERE codigo = 'COCINA';
