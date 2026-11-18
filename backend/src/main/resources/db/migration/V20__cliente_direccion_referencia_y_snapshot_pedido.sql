-- Referencia del domicilio y snapshot de la dirección en el pedido.
ALTER TABLE comercial.clientes_direcciones
    ADD COLUMN IF NOT EXISTS referencia VARCHAR(200);

ALTER TABLE comercial.pedidos_clientes
    ADD COLUMN IF NOT EXISTS direccion_texto VARCHAR(200),
    ADD COLUMN IF NOT EXISTS referencia VARCHAR(200),
    ADD COLUMN IF NOT EXISTS observaciones VARCHAR(500),
    ADD COLUMN IF NOT EXISTS lat NUMERIC(10,7),
    ADD COLUMN IF NOT EXISTS lng NUMERIC(10,7);