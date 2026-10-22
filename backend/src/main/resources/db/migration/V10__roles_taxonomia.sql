-- =====================================================================
-- Taxonomía de roles: categorías, banderas y modelo vigente.
--
-- Este archivo consolida TODO el rediseño de roles en una sola migración
-- (antes eran varias que se pisaban entre sí: se fusionaba MESERO y
-- COCINA, se deshacía, se recreaba...). Deja la base así:
--
--   CATEGORÍA  ROLES QUE EL ADMIN PUEDE ASIGNAR
--   ---------  --------------------------------
--   ADMIN      (ninguno: la cuenta inicial del seed)
--   EMPLEADO   MESERO | CAJERO
--   CLIENTE    (ninguno: auto-registro en la app móvil)
--   DELIVERY   (ninguno: auto-registro en la app móvil)
--
-- Piezas:
--
--  1. Permisos del flujo de entrega (pedidos:entregar y
--     pedidos:retiro-mostrador) y la tabla de permisos EXPLÍCITOS por
--     usuario (seguridad.usuarios_permisos): si un usuario tiene alguna
--     fila ahí, rigen solo esos permisos y no los de su rol.
--
--  2. COCINA deja de ser un rol: la cocina pasa a ser una funcionalidad
--     (abrir comandas y marcar la preparación) activable por el
--     administrador sobre cualquier empleado. Los cocineros pasan a MESERO
--     conservando su acceso (como permisos explícitos) y sus sesiones
--     abiertas se invalidan para que vuelva a entrar.
--
--  3. DELIVERY (repartidor) y CAJERO (cobro, caja, facturación, finanzas
--     y reportes) con sus permisos por defecto.
--
--  4. Categorías de roles y banderas por rol: lo que el administrador
--     puede asignar lo decide la BASE DE DATOS, no una lista en el código.
--
--  5. ADMIN tiene todos los permisos por regla y no por lista: se
--     resincroniza con el catálogo y un trigger lo mantiene solo.
--
--  6. Celular de contacto del usuario (auto-registro de repartidor).
--
-- Idempotente: se puede reaplicar sobre una base que ya tenía el modelo
-- viejo sin romper nada. Los ids de los roles NO se fuerzan: cambian
-- respecto de las bases migradas archivo por archivo y da igual, el
-- código siempre habla por `codigo`.
-- =====================================================================

-- ---------------------------------------------------------------
-- 1. Permisos del flujo de entrega
-- ---------------------------------------------------------------
INSERT INTO seguridad.permisos (codigo, modulo, descripcion) VALUES
 ('pedidos:entregar',         'pedidos', 'Marcar pedido como entregado'),
 ('pedidos:retiro-mostrador', 'pedidos', 'Confirmar retiro de pedido en mostrador')
ON CONFLICT (codigo) DO NOTHING;

SELECT setval('seguridad.permisos_id_seq', (SELECT MAX(id) FROM seguridad.permisos));

-- ---------------------------------------------------------------
-- 2. Permisos explícitos por usuario
--
--    Si el usuario no tiene filas, rigen los permisos por defecto de su
--    rol (roles_permisos). Si tiene alguna, rigen SOLO esos: por eso el
--    set explícito de los cocineros migrados (más abajo) es completo.
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS seguridad.usuarios_permisos (
    usuario_id BIGINT NOT NULL REFERENCES seguridad.usuarios (id) ON DELETE CASCADE,
    permiso_id BIGINT NOT NULL REFERENCES seguridad.permisos (id) ON DELETE CASCADE,
    PRIMARY KEY (usuario_id, permiso_id)
);

CREATE INDEX IF NOT EXISTS idx_usuarios_permisos_usuario
    ON seguridad.usuarios_permisos (usuario_id);

-- ---------------------------------------------------------------
-- 3. COCINA deja de ser un rol: la cocina pasa a ser una funcionalidad
--
--    Los cocineros pasan a MESERO (su subrol en la categoría EMPLEADO)
--    conservando su acceso: como MESERO no trae los permisos de cocina
--    (pedidos:estado-preparacion, comandas:reenviar), se le guardan como
--    permisos EXPLÍCITOS. El set es COMPLETO a propósito: si el usuario
--    tiene cualquier permiso explícito, rigen solo esos y no los del rol.
-- ---------------------------------------------------------------
CREATE TEMP TABLE migrados_cocina ON COMMIT DROP AS
SELECT u.id
FROM seguridad.usuarios u
JOIN seguridad.roles r ON r.id = u.rol_id
WHERE r.codigo = 'COCINA';

-- Permisos del subrol MESERO
INSERT INTO seguridad.usuarios_permisos (usuario_id, permiso_id)
SELECT m.id, rp.permiso_id
FROM migrados_cocina m
JOIN seguridad.roles r ON r.codigo = 'MESERO'
JOIN seguridad.roles_permisos rp ON rp.rol_id = r.id
ON CONFLICT DO NOTHING;

-- Permisos de cocina que MESERO no trae (los que el usuario tenía)
INSERT INTO seguridad.usuarios_permisos (usuario_id, permiso_id)
SELECT m.id, rp.permiso_id
FROM migrados_cocina m
JOIN seguridad.roles r ON r.codigo = 'COCINA'
JOIN seguridad.roles_permisos rp ON rp.rol_id = r.id
ON CONFLICT DO NOTHING;

-- Cambio de rol: COCINA -> MESERO
UPDATE seguridad.usuarios u
SET rol_id    = (SELECT id FROM seguridad.roles WHERE codigo = 'MESERO'),
    updated_at = now(),
    updated_by = 'migracion-roles'
FROM migrados_cocina m
WHERE u.id = m.id;

-- Las sesiones abiertas siguen llevando los permisos de COCINA en el
-- token: se invalida la versión de sesión para forzar el reingreso.
UPDATE seguridad.usuarios u
SET sesion_version = u.sesion_version + 1
FROM migrados_cocina m
WHERE u.id = m.id;

-- Baja del rol COCINA
DELETE FROM seguridad.roles_permisos
WHERE rol_id = (SELECT id FROM seguridad.roles WHERE codigo = 'COCINA');

DELETE FROM seguridad.roles WHERE codigo = 'COCINA';

-- ---------------------------------------------------------------
-- 4. El rol DELIVERY (repartidor)
--
--    Nace por auto-registro desde la app móvil, no asignable por el
--    administrador (eso lo decide la bandera de más abajo).
-- ---------------------------------------------------------------
INSERT INTO seguridad.roles (codigo, descripcion)
SELECT 'DELIVERY', 'Repartidor: entrega pedidos y confirma retiros'
WHERE NOT EXISTS (SELECT 1 FROM seguridad.roles WHERE codigo = 'DELIVERY');

-- Permisos por defecto: ver el flujo de pedidos de entrega y consultar
-- catálogo y mesas.
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo IN (
    'catalogo:ver', 'mesas:ver',
    'pedidos:ver', 'pedidos:entregar', 'pedidos:retiro-mostrador')
WHERE r.codigo = 'DELIVERY'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------
-- 5. El rol CAJERO (segundo subrol de la categoría EMPLEADO)
--
--    Nace asignable y configurable: el administrador puede crearlo con
--    estos permisos por defecto y quitarle o agregarle los que necesite.
-- ---------------------------------------------------------------
INSERT INTO seguridad.roles (codigo, descripcion)
SELECT 'CAJERO', 'Cobra, maneja caja y emite comprobantes'
WHERE NOT EXISTS (SELECT 1 FROM seguridad.roles WHERE codigo = 'CAJERO');

-- Permisos por defecto: cobro, caja, facturación, finanzas y reportes.
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo IN (
    'catalogo:ver',
    'mesas:ver',
    'pedidos:ver',
    'comandas:ver',
    'anulaciones:solicitar',
    'anulaciones:aprobar',
    'cuentas:ver',
    'cuentas:gestionar',
    'pagos:cobrar',
    'facturacion:emitir',
    'caja:apertura',
    'caja:movimientos',
    'caja:cierre',
    'finanzas:ver',
    'reportes:ver')
WHERE r.codigo = 'CAJERO'
ON CONFLICT DO NOTHING;

SELECT setval('seguridad.roles_id_seq', (SELECT MAX(id) FROM seguridad.roles));

-- ---------------------------------------------------------------
-- 6. Categorías de roles
--
--    La categoría no concede permisos por sí misma: solo organiza y
--    permite agrupar en el panel.
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
-- 7. Banderas por rol
--
--    asignable_por_admin: si es FALSE, el panel NO puede crear ni
--      asignar ese rol (el backend lo rechaza con 422).
--    configurable: si es TRUE, el administrador puede agregar o quitar
--      funcionalidades (permisos explícitos en usuarios_permisos).
--    origen: de dónde nace la cuenta (ADMIN | AUTO_REGISTRO).
-- ---------------------------------------------------------------
ALTER TABLE seguridad.roles
    ADD COLUMN IF NOT EXISTS categoria_id        BIGINT REFERENCES seguridad.categorias_roles (id),
    ADD COLUMN IF NOT EXISTS asignable_por_admin BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS configurable        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS origen              VARCHAR(20) NOT NULL DEFAULT 'ADMIN';

-- Backfill: cada rol a su categoría
UPDATE seguridad.roles SET categoria_id = (SELECT id FROM seguridad.categorias_roles WHERE codigo = 'ADMIN')
WHERE codigo = 'ADMIN';

UPDATE seguridad.roles SET categoria_id = (SELECT id FROM seguridad.categorias_roles WHERE codigo = 'EMPLEADO')
WHERE codigo IN ('MESERO', 'CAJERO');

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

-- ADMIN: una sola cuenta (la inicial del seed); no se asigna desde el panel.
UPDATE seguridad.roles
SET asignable_por_admin = FALSE, configurable = FALSE, origen = 'ADMIN'
WHERE codigo = 'ADMIN';

-- CLIENTE y DELIVERY: solo nacen por auto-registro desde el móvil.
UPDATE seguridad.roles
SET asignable_por_admin = FALSE, configurable = FALSE, origen = 'AUTO_REGISTRO'
WHERE codigo IN ('CLIENTE', 'DELIVERY');

-- MESERO y CAJERO: el administrador ajusta sus funcionalidades.
UPDATE seguridad.roles
SET asignable_por_admin = TRUE, configurable = TRUE, origen = 'ADMIN'
WHERE codigo IN ('MESERO', 'CAJERO');

-- ---------------------------------------------------------------
-- 8. El ADMIN tiene todos los permisos, por regla y no por lista
--
--    ADMIN arrastraba una lista que se desincronizaba cada vez que se
--    creaba un permiso nuevo (le faltaban los dos de entrega). Aquí se
--    resincroniza con el catálogo completo y se instala un trigger que lo
--    mantiene solo: cada permiso nuevo nace granting a ADMIN, y si un
--    permiso se desactiva deja de estar en ADMIN (y vuelve si se reactiva).
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

-- ---------------------------------------------------------------
-- 9. Celular de contacto del usuario
--
--    El auto-registro de repartidor (rol DELIVERY) captura el celular con
--    el que el restaurante lo contacta. La columna es opcional: solo la
--    usan las cuentas que se auto-registran (hoy, los repartidores); el
--    resto se contactan por correo. No es única: dos repartidores pueden
--    compartir un número (turnos, líneas compartidas).
-- ---------------------------------------------------------------
ALTER TABLE seguridad.usuarios
    ADD COLUMN IF NOT EXISTS celular VARCHAR(20);
