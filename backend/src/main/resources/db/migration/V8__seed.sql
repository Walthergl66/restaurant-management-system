-- =====================================================================
-- MIGRACIÓN CONSOLIDADA (squash del V1..V16 original).
-- Datos iniciales: permisos, roles, asignaciones y parámetros por defecto.
-- (Equivale a schemas/08_seed.sql del entregable de la práctica.)
-- =====================================================================

-- Datos iniciales del sistema (semillas de V1, V2, V6 y V9 del modelo
-- original): permisos, roles, asignaciones rol↔permiso y parámetros por
-- defecto, para que la base de datos nazca funcional.

-- ---------------------------------------------------------------
-- Permisos (codigo: <modulo>:<accion>)
-- ---------------------------------------------------------------
INSERT INTO seguridad.permisos (id, codigo, modulo, descripcion) VALUES
 (1,  'usuarios:ver',       'usuarios', 'Consultar usuarios'),
 (2,  'usuarios:crear',     'usuarios', 'Crear usuarios'),
 (3,  'usuarios:editar',    'usuarios', 'Editar usuarios'),
 (4,  'usuarios:eliminar',  'usuarios', 'Desactivar usuarios'),
 (5,  'roles:asignar',      'usuarios', 'Asignar roles a usuarios'),
 (6,  'catalogo:ver',       'catalogo', 'Ver catálogo (productos, extras, áreas)'),
 (7,  'catalogo:gestionar', 'catalogo', 'Administrar catálogo'),
 (8,  'mesas:ver',          'mesas',    'Ver mesas y su estado'),
 (9,  'mesas:gestionar',    'mesas',    'Administrar mesas'),
 (10, 'pedidos:ver',        'pedidos',  'Ver pedidos'),
 (11, 'pedidos:crear',      'pedidos',  'Crear borradores de pedido'),
 (12, 'pedidos:editar',     'pedidos',  'Editar borradores de pedido'),
 (13, 'pedidos:confirmar',  'pedidos',  'Confirmar pedidos'),
 (14, 'pedidos:estado-preparacion', 'pedidos', 'Marcar pedido en preparación'),
 (15, 'pedidos:estado-listo',        'pedidos', 'Marcar pedido listo/entregado'),
 (16, 'comandas:ver',       'comandas', 'Ver comandas'),
 (17, 'comandas:reenviar',  'comandas', 'Reenviar comandas'),
 (18, 'anulaciones:solicitar', 'anulaciones', 'Solicitar anulación de línea'),
 (19, 'anulaciones:aprobar',   'anulaciones', 'Aprobar o rechazar anulaciones'),
 (20, 'cuentas:ver',        'cuentas',  'Ver cuentas de mesa'),
 (21, 'cuentas:gestionar',  'cuentas',  'Administrar cuentas'),
 (22, 'pagos:cobrar',       'pagos',    'Registrar cobros'),
 (23, 'facturacion:emitir', 'facturacion', 'Emitir comprobantes'),
 (24, 'caja:apertura',      'caja',     'Abrir caja'),
 (25, 'caja:movimientos',   'caja',     'Registrar movimientos de caja'),
 (26, 'caja:cierre',        'caja',     'Cerrar caja'),
 (27, 'finanzas:ver',       'finanzas', 'Ver finanzas'),
 (28, 'reportes:ver',       'reportes', 'Generar reportes'),
 (29, 'auditoria:ver',      'auditoria', 'Consultar auditoría'),
 (30, 'clientes:gestionar', 'clientes', 'Administrar clientes'),
 (31, 'configuracion:gestionar', 'configuracion', 'Administrar configuración'),
 (32, 'clientes:menu-ver',           'clientes', 'Ver menú público desde la app'),
 (33, 'clientes:carrito-gestionar',  'clientes', 'Gestionar carrito de compra'),
 (34, 'clientes:pedido-crear',       'clientes', 'Crear pedido desde la app'),
 (35, 'clientes:pedido-confirmar',   'clientes', 'Confirmar pedido con idempotencia'),
 (36, 'clientes:pedido-estado-ver',  'clientes', 'Ver estado del pedido en tiempo real'),
 (37, 'clientes:historial-ver',      'clientes', 'Ver historial de pedidos'),
 (38, 'clientes:cuenta-nueva',       'clientes', 'Registrar/recuperar cuenta de cliente');

-- ---------------------------------------------------------------
-- Roles
-- ---------------------------------------------------------------
INSERT INTO seguridad.roles (id, codigo, descripcion) VALUES
 (1, 'ADMIN',   'Administrador del sistema'),
 (2, 'MESERO',  'Mesero con acceso al flujo de pedidos'),
 (3, 'COCINA',  'Personal de cocina'),
 (4, 'CAJERO',  'Cajero que cobra y maneja caja'),
 (5, 'CLIENTE', 'Cliente de la app móvil con QR');

-- ---------------------------------------------------------------
-- Asignaciones rol ↔ permiso
-- ---------------------------------------------------------------
-- ADMIN: todos los permisos
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT 1, id FROM seguridad.permisos;

-- MESERO
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id) VALUES
 (2, 6), (2, 8), (2, 10), (2, 11), (2, 12), (2, 13), (2, 15),
 (2, 16), (2, 18), (2, 20), (2, 21), (2, 30);

-- COCINA
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id) VALUES
 (3, 6), (3, 10), (3, 14), (3, 15), (3, 16), (3, 17), (3, 18);

-- CAJERO (incluye cuentas:gestionar concedida en V6)
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id) VALUES
 (4, 6), (4, 8), (4, 10), (4, 16), (4, 18), (4, 19), (4, 20),
 (4, 21), (4, 22), (4, 23), (4, 24), (4, 25), (4, 26), (4, 27), (4, 28);

-- CLIENTE
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id) VALUES
 (5, 32), (5, 33), (5, 34), (5, 35), (5, 36), (5, 37), (5, 38);

-- Las inserciones usaron ids explícitos: se pone la secuencia al día.
SELECT setval('seguridad.roles_id_seq', (SELECT MAX(id) FROM seguridad.roles));
SELECT setval('seguridad.permisos_id_seq', (SELECT MAX(id) FROM seguridad.permisos));

-- ---------------------------------------------------------------
-- Parámetros por defecto del restaurante
-- ---------------------------------------------------------------
INSERT INTO configuracion.parametros (clave, valor, tipo, descripcion) VALUES
 ('restaurant.nombre', 'Restaurante',  'TEXTO',    'Nombre del restaurante que aparece en el menú público'),
 ('restaurant.direccion', '',          'TEXTO',    'Dirección del restaurante'),
 ('restaurant.telefono', '',           'TEXTO',    'Teléfono del restaurante'),
 ('impuestos.iva', '0.15',             'NUMERICO', 'IVA en tanto por uno, incluido en los precios de catálogo'),
 ('moneda', 'USD',                     'TEXTO',    'Código de moneda para precios y comprobantes');
