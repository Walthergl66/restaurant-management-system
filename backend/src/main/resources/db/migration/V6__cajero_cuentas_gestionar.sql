-- V6: permiso cuentas:gestionar para el rol CAJERO
-- (el cajero cobra y cierra cuentas; en V1 solo quedó asignado al mesero).
-- No se edita V1 porque ya está aplicada: se concede aquí de forma aditiva.
INSERT INTO roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r, permisos p
WHERE r.codigo = 'CAJERO' AND p.codigo = 'cuentas:gestionar'
  AND NOT EXISTS (
      SELECT 1 FROM roles_permisos rp
      WHERE rp.rol_id = r.id AND rp.permiso_id = p.id
  );