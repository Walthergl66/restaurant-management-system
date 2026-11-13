-- =====================================================================
-- Recargo de entrega congelado en el pedido del cliente.
-- =====================================================================
-- Guarda el costo de entrega que se CONGELA al confirmar el pedido:
--   * 0.00 para pedidos RETIRAR (sin recargo),
--   * el recargo configurado (p. ej. 1.50) para pedidos DOMICILIO.
-- El total que paga el cliente = suma de subtotales de líneas + costo_entrega.
-- Se persiste en el backend para que no pueda alterarse desde el cliente y
-- para que un reintento idempotente de la confirmación NO lo vuelva a sumar.
ALTER TABLE comercial.pedidos_clientes
    ADD COLUMN costo_entrega NUMERIC(12,2) NOT NULL DEFAULT 0.00;

COMMENT ON COLUMN comercial.pedidos_clientes.costo_entrega IS
    'Recargo de entrega congelado al confirmar: 0.00 para RETIRAR, el recargo configurado (1.50) para DOMICILIO. El backend lo calcula y persiste; no se duplica en reintentos idempotentes.';
