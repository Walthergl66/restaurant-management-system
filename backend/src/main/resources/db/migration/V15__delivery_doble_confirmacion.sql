-- =====================================================================
-- Schema: DELIVERY — Doble confirmación de entrega (repartidor + cliente).
--
-- Una entrega se considera VERIFICADA solo cuando existen DOS confirmaciones
-- independientes: la del repartidor (llegó/entregó) y la del cliente (recibí).
-- Estos datos viven en la PARADA, no en la asignación ni en el recorrido,
-- porque cada parada representa la entrega de UN pedido: así sirve igual para
-- la entrega individual y para cada parada de una multientrega (cada parada
-- lleva su propia confirmación).
--
-- IMPORTANTE — cuatro conceptos SEPARADOS, no se mezclan:
--   1. Confirmación del repartidor -> repartidor_confirmo_at / _por.
--   2. Confirmación del cliente     -> cliente_confirmo_at.
--   3. Estado LOGÍSTICO (parada.estado PENDIENTE/ENTREGADA/FALLIDA y el estado
--      de la asignación ASIGNADA/EN_RUTA/ENTREGADA/FALLIDA): es el avance
--      operativo del recorrido, NO la verificación de la entrega.
--   4. Estado FINAL verificado    -> entrega_verificada (flag aparte).
--
-- Ninguna de estas columnas toca el estado de PREPARACIÓN del pedido en
-- comercial: la verificación de entrega es ajena a la máquina de estados del
-- pedido del cliente.
--
-- resuelta_por_admin / resuelta_motivo registran, cuando el cliente nunca
-- confirma, la resolución AUDITADA y justificada del administrador (no hay
-- cierre automático por tiempo: solo acción humana con motivo).
-- =====================================================================

ALTER TABLE delivery.paradas
    ADD COLUMN repartidor_confirmo_at  TIMESTAMPTZ,
    ADD COLUMN repartidor_confirmo_por VARCHAR(50),
    ADD COLUMN cliente_confirmo_at     TIMESTAMPTZ,
    ADD COLUMN entrega_verificada      BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN resuelta_por_admin      VARCHAR(50),
    ADD COLUMN resuelta_motivo         VARCHAR(300);

COMMENT ON COLUMN delivery.paradas.repartidor_confirmo_at IS
    'Fecha/hora en que el repartidor confirmó la llegada/entrega (concepto separado del estado logístico).';
COMMENT ON COLUMN delivery.paradas.repartidor_confirmo_por IS
    'Usuario repartidor que confirmó la entrega (auditoría de la confirmación 1 de 2).';
COMMENT ON COLUMN delivery.paradas.cliente_confirmo_at IS
    'Fecha/hora en que el cliente confirmó la recepción (confirmación 2 de 2); null mientras esté pendiente.';
COMMENT ON COLUMN delivery.paradas.entrega_verificada IS
    'Estado FINAL verificado: true solo cuando existen ambas confirmaciones (o resolución auditada del admin). Flag independiente del estado logístico.';
COMMENT ON COLUMN delivery.paradas.resuelta_por_admin IS
    'Administrador que resolvió la confirmación pendiente del cliente (incidencia); null si no hubo resolución.';
COMMENT ON COLUMN delivery.paradas.resuelta_motivo IS
    'Motivo justificado de la resolución administrativa de una confirmación pendiente del cliente.';

-- ---------------------------------------------------------------
-- Permiso del admin para RESOLVER incidencias de confirmación pendiente del
-- cliente (patrón de V13/V14: alta idempotente y asignación explícita a ADMIN).
-- Se separa de 'delivery:asignaciones-gestionar' porque es una acción distinta
-- (cerrar una entrega que el cliente nunca confirmó, con motivo y auditoría).
-- ---------------------------------------------------------------
INSERT INTO seguridad.permisos (codigo, modulo, descripcion) VALUES
 ('delivery:confirmacion-resolver', 'delivery', 'Admin: resolver entregas pendientes de confirmacion del cliente (auditada)')
ON CONFLICT (codigo) DO NOTHING;

SELECT setval('seguridad.permisos_id_seq', (SELECT MAX(id) FROM seguridad.permisos));

INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo = 'delivery:confirmacion-resolver'
WHERE r.codigo = 'ADMIN'
ON CONFLICT DO NOTHING;
