-- =====================================================================
-- CAJERO pasa a ser el rol operativo con acceso total (equivalente a
-- ADMIN): se le conceden TODOS los permisos existentes. Aditivo e
-- idempotente (ON CONFLICT), no toca la asignación original del seed.
-- =====================================================================

INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
CROSS JOIN seguridad.permisos p
WHERE r.codigo = 'CAJERO'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
