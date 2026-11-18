-- =====================================================================
-- Permisos del módulo DELIVERY (schema seguridad).
--
-- Sigue el patrón de V10__roles_taxonomia.sql: inserción idempotente de
-- permisos (ON CONFLICT DO NOTHING) y asignación a roles vía
-- INSERT ... SELECT ... WHERE ... ON CONFLICT DO NOTHING.
--
-- Dos grupos de permisos, a propósito separados:
--
--  1. OPERATIVOS (los usa el REPARTIDOR en la app móvil):
--       delivery:mis-entregas     -> ver sus asignaciones y recorridos.
--       delivery:recorrido-operar -> iniciar recorrido, marcar parada
--                                    entregada, registrar incidencia y
--                                    finalizar.
--     Se asignan al rol DELIVERY.
--
--  2. ADMINISTRATIVOS (los usa el ADMIN):
--       delivery:repartidores-gestionar -> habilitar/suspender y listar
--                                          repartidores.
--       delivery:asignaciones-gestionar -> asignar/reasignar pedidos y
--                                          consultar estados de reparto.
--     Se insertan en seguridad.permisos.
--
-- IMPORTANTE sobre el estado operativo: tener el permiso NO basta. El
-- permiso dice QUÉ puede hacer el rol; el estado del repartidor
-- (delivery.repartidores.estado = HABILITADO) dice si HOY opera. Un
-- repartidor con el permiso delivery:recorrido-operar pero en estado
-- PENDIENTE o SUSPENDIDO NO puede operar: el estado es independiente del
-- permiso y se verifica aparte en la capa de aplicación.
--
-- Sobre ADMIN: en V10 el rol ADMIN se siembra con TODOS los permisos y,
-- además, un trigger (trg_admin_todos_los_permisos) le concede cada
-- permiso nuevo al insertarlo. Aun así, aquí se añade la asignación
-- EXPLÍCITA de los 4 permisos nuevos al rol ADMIN (idempotente) para que
-- la migración deje garantizado el estado final sin depender del trigger.
-- =====================================================================

-- ---------------------------------------------------------------
-- 1. Alta de los permisos del módulo delivery (operativos + admin)
-- ---------------------------------------------------------------
INSERT INTO seguridad.permisos (codigo, modulo, descripcion) VALUES
 ('delivery:mis-entregas',           'delivery', 'Repartidor: ver sus asignaciones y recorridos'),
 ('delivery:recorrido-operar',       'delivery', 'Repartidor: iniciar recorrido, marcar entregas, incidencias y finalizar'),
 ('delivery:repartidores-gestionar', 'delivery', 'Admin: habilitar/suspender y listar repartidores'),
 ('delivery:asignaciones-gestionar', 'delivery', 'Admin: asignar/reasignar pedidos y consultar estados de reparto')
ON CONFLICT (codigo) DO NOTHING;

SELECT setval('seguridad.permisos_id_seq', (SELECT MAX(id) FROM seguridad.permisos));

-- ---------------------------------------------------------------
-- 2. Permisos OPERATIVOS al rol DELIVERY
--
--    El repartidor opera su propio flujo; la gestión administrativa NO se
--    le concede. El estado HABILITADO se verifica aparte en la aplicación.
-- ---------------------------------------------------------------
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo IN (
    'delivery:mis-entregas',
    'delivery:recorrido-operar')
WHERE r.codigo = 'DELIVERY'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------
-- 3. Los 4 permisos nuevos al rol ADMIN (explícito e idempotente)
--
--    Redundante con el trigger de V10, pero deja el estado final
--    garantizado por la propia migración.
-- ---------------------------------------------------------------
INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo IN (
    'delivery:mis-entregas',
    'delivery:recorrido-operar',
    'delivery:repartidores-gestionar',
    'delivery:asignaciones-gestionar')
WHERE r.codigo = 'ADMIN'
ON CONFLICT DO NOTHING;
