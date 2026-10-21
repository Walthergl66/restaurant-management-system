-- =====================================================================
-- Taxonomía de roles: categorías de primer nivel y banderas por rol.
--
--  - seguridad.categorias_roles agrupa los roles en categorías:
--      ADMIN    -> administración del sistema
--      EMPLEADO -> personal interno (subroles MESERO | CAJERO)
--      CLIENTE  -> clientes de la app móvil
--      DELIVERY -> repartidores
--    Los subroles cuelgan de una categoría; la categoría no concede
--    permisos por sí misma, solo organiza y permite agrupar en el panel.
--
--  - roles.asignable_por_admin: si es FALSE, el panel NO puede crear ni
--    asignar ese rol (se rechaza con 422):
--      ADMIN    -> existe una sola cuenta, la inicial; no se replicas
--      CLIENTE  -> solo nacen por auto-registro desde el móvil
--      DELIVERY -> solo nacen por auto-registro desde el móvil
--
--  - roles.configurable: si es TRUE, el administrador puede agregar o
--    quitar funcionalidades (permisos explícitos en usuarios_permisos)
--    de los usuarios con ese rol. Si el usuario no tiene permisos
--    explícitos, rigen los defaults del rol (roles_permisos).
--
--  - roles.origen: de dónde nace la cuenta (ADMIN | AUTO_REGISTRO).
--
--  Migración aditiva e idempotente: no borra ni altera datos de negocio,
--  solo añade columnas, crea la tabla de categorías y su backfill.
-- =====================================================================

-- ---------------------------------------------------------------
-- Categorías de roles
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS seguridad.categorias_roles (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(30)  NOT NULL UNIQUE,
    descripcion VARCHAR(120)
);

INSERT INTO seguridad.categorias_roles (codigo, descripcion) VALUES
 ('ADMIN',    'Administración del sistema'),
 ('EMPLEADO', 'Personal interno del local'),
 ('CLIENTE',  'Clientes de la app móvil'),
 ('DELIVERY', 'Repartidores')
ON CONFLICT (codigo) DO NOTHING;

-- ---------------------------------------------------------------
-- Banderas por rol
-- ---------------------------------------------------------------
ALTER TABLE seguridad.roles
    ADD COLUMN IF NOT EXISTS categoria_id        BIGINT REFERENCES seguridad.categorias_roles (id),
    ADD COLUMN IF NOT EXISTS asignable_por_admin BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS configurable        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS origen              VARCHAR(20) NOT NULL DEFAULT 'ADMIN';

-- ---------------------------------------------------------------
-- Backfill: cada rol a su categoría
-- ---------------------------------------------------------------
UPDATE seguridad.roles SET categoria_id = (SELECT id FROM seguridad.categorias_roles WHERE codigo = 'ADMIN')
WHERE codigo = 'ADMIN';

UPDATE seguridad.roles SET categoria_id = (SELECT id FROM seguridad.categorias_roles WHERE codigo = 'EMPLEADO')
WHERE codigo IN ('MESERO', 'COCINA');

UPDATE seguridad.roles SET categoria_id = (SELECT id FROM seguridad.categorias_roles WHERE codigo = 'CLIENTE')
WHERE codigo = 'CLIENTE';

UPDATE seguridad.roles SET categoria_id = (SELECT id FROM seguridad.categorias_roles WHERE codigo = 'DELIVERY')
WHERE codigo = 'DELIVERY';

-- La categoría es obligatoria, pero solo se endurece si no quedó ningún
-- rol sin categoría: así una base con roles inesperados no rompe la
-- migración (quedaría con categoria_id NULL y la app lo reportaría).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM seguridad.roles WHERE categoria_id IS NULL) THEN
        ALTER TABLE seguridad.roles ALTER COLUMN categoria_id SET NOT NULL;
    END IF;
END $$;

-- ---------------------------------------------------------------
-- Asignabilidad y configurabilidad por rol
-- ---------------------------------------------------------------
-- ADMIN: una sola cuenta (la inicial del seed); no se asigna desde el panel.
UPDATE seguridad.roles
SET asignable_por_admin = FALSE, configurable = FALSE, origen = 'ADMIN'
WHERE codigo = 'ADMIN';

-- CLIENTE y DELIVERY: solo nacen por auto-registro desde el móvil.
UPDATE seguridad.roles
SET asignable_por_admin = FALSE, configurable = FALSE, origen = 'AUTO_REGISTRO'
WHERE codigo IN ('CLIENTE', 'DELIVERY');

-- MESERO y COCINA: el administrador ajusta sus funcionalidades.
-- (COCINA se da de baja en V17__migrar_cocina_a_mesero.sql.)
UPDATE seguridad.roles
SET asignable_por_admin = TRUE, configurable = TRUE, origen = 'ADMIN'
WHERE codigo IN ('MESERO', 'COCINA');
