-- =====================================================================
-- CAJERO deja de existir como rol: sus funciones pasan al rol ADMIN.
-- Se reasignan los usuarios que lo tuvieran (defensivo: en el modelo actual
-- ningún seed crea usuarios) y se borra el rol con sus asignaciones de
-- permisos. Idempotente/defensivo: si el rol no existe, no hace nada.
-- =====================================================================

UPDATE seguridad.usuarios
SET rol_id = (SELECT id FROM seguridad.roles WHERE codigo = 'ADMIN')
WHERE rol_id = (SELECT id FROM seguridad.roles WHERE codigo = 'CAJERO');

DELETE FROM seguridad.roles_permisos
WHERE rol_id = (SELECT id FROM seguridad.roles WHERE codigo = 'CAJERO');

DELETE FROM seguridad.roles WHERE codigo = 'CAJERO';
