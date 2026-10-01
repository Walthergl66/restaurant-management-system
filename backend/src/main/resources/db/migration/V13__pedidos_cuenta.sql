-- =====================================================================
-- V13__pedidos_cuenta.sql. ADITIVA sobre V1..V12.
-- Pertenencia explícita pedido -> cuenta. Antes, la cuenta se totalizaba con
-- TODOS los pedidos no anulados de la mesa, en cualquier turno: un pedido ya
-- cobrado en un turno se volvía a sumar en la cuenta del turno siguiente
-- (doble cobro). La columna fija la pertenencia y el total se calcula por
-- cuenta, no por mesa.
ALTER TABLE pedidos ADD COLUMN cuenta_id BIGINT REFERENCES cuentas (id);

CREATE INDEX idx_pedidos_cuenta ON pedidos (cuenta_id);

-- Backfill: los pedidos no anulados de una mesa con cuenta abierta pasan a
-- esa cuenta. Las cuentas cerradas históricas no se reasignan (no hay forma
-- fiable de saber a qué turno pertenecían).
UPDATE pedidos p
SET cuenta_id = c.id
FROM cuentas c
WHERE c.mesa_id = p.mesa_id
  AND c.estado = 'ABIERTA'
  AND p.estado <> 'ANULADO'
  AND p.cuenta_id IS NULL;
