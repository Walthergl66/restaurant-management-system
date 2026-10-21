-- =====================================================================
-- El ADMIN tiene todos los permisos, por regla y no por lista.
--
--  - Hoy ADMIN arrastra una lista de permisos que se desincroniza cada vez
--    que se crea uno nuevo (le faltan pedidos:entregar y
--    pedidos:retiro-mostrador). La lista es el problema: nadie la
--    mantiene.
--
--  - Aquí ADMIN se resincroniza con el catálogo completo de permisos y se
--    instala un trigger que se encarga de mantenerlo solo: cada permiso
--    nuevo nace granting a ADMIN, y si un permiso se desactiva deja de
--    estar en ADMIN (y vuelve si se reactiva).
--
--  - Así el ADMIN es "el que puede todo" por definición, sin depender de
--    una migración por cada permiso nuevo.
--
--  - Migración aditiva e idempotente.
-- =====================================================================

-- ---------------------------------------------------------------
-- Resincronización: ADMIN se queda con todos los permisos activos
-- ---------------------------------------------------------------
DELETE FROM seguridad.roles_permisos rp
USING seguridad.roles r
WHERE rp.rol_id = r.id
  AND r.codigo = 'ADMIN';

INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
CROSS JOIN seguridad.permisos p
WHERE r.codigo = 'ADMIN'
  AND p.activo
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------
-- Trigger de mantenimiento: ADMIN sigue al catálogo de permisos
-- ---------------------------------------------------------------
CREATE OR REPLACE FUNCTION seguridad.fn_admin_todos_los_permisos() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.activo THEN
        INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
        SELECT r.id, NEW.id
        FROM seguridad.roles r
        WHERE r.codigo = 'ADMIN'
        ON CONFLICT DO NOTHING;
    ELSE
        DELETE FROM seguridad.roles_permisos rp
        USING seguridad.roles r
        WHERE rp.rol_id = r.id
          AND r.codigo = 'ADMIN'
          AND rp.permiso_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_admin_todos_los_permisos ON seguridad.permisos;

CREATE TRIGGER trg_admin_todos_los_permisos
    AFTER INSERT OR UPDATE ON seguridad.permisos
    FOR EACH ROW
    EXECUTE FUNCTION seguridad.fn_admin_todos_los_permisos();
