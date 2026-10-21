-- =====================================================================
-- Subrol CAJERO de la categoría EMPLEADO.
--
--  - CAJERO es el segundo subrol de EMPLEADO (junto a MESERO). Recupera el
--    perfil del cajero que V8__seed.sql eliminó (sus funciones pasaron al
--    ADMIN) y que la baja de CAJERO dejó sin cobertura: cobro, caja,
--    facturación, finanzas y reportes.
--
--  - Nace asignable y configurable: el administrador puede crear al cajero
--    con estos permisos por defecto y quitarle o agregarle los que necesite.
--
--  - Migración aditiva e idempotente.
-- =====================================================================

-- ---------------------------------------------------------------
-- El rol CAJERO
-- ---------------------------------------------------------------
-- La categoría es obligatoria (NOT NULL desde V15), así que se inserta
-- junto con el rol: la categoría EMPLEADO es única y el ON CONFLICT
-- evita duplicar el rol si la migración se reaplica.
INSERT INTO seguridad.roles (codigo, descripcion, categoria_id, asignable_por_admin, configurable, origen)
SELECT 'CAJERO', 'Cobra, maneja caja y emite comprobantes', c.id, TRUE, TRUE, 'ADMIN'
FROM seguridad.categorias_roles c
WHERE c.codigo = 'EMPLEADO'
ON CONFLICT (codigo) DO NOTHING;

-- Normaliza las banderas por si el rol ya existía con otros valores.
UPDATE seguridad.roles
SET categoria_id        = (SELECT id FROM seguridad.categorias_roles WHERE codigo = 'EMPLEADO'),
    asignable_por_admin = TRUE,
    configurable        = TRUE,
    origen              = 'ADMIN'
WHERE codigo = 'CAJERO';

-- ---------------------------------------------------------------
-- Permisos por defecto de CAJERO
-- (los mismos del cajero eliminado en V8__seed.sql)
-- ---------------------------------------------------------------
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
