-- =====================================================================
-- Teléfono opcional en el perfil del cliente.
-- =====================================================================
-- El teléfono deja de ser obligatorio en el registro del cliente: puede
-- quedar NULL hasta que se capture un teléfono de contacto en el checkout a
-- domicilio. NO se eliminan placeholders ni se generan teléfonos falsos.
--
-- Se CONSERVA la restricción UNIQUE: en PostgreSQL varios valores NULL no se
-- consideran iguales entre sí, así que múltiples clientes sin teléfono (NULL)
-- conviven sin violar el UNIQUE; el UNIQUE sigue impidiendo teléfonos
-- repetidos entre los clientes que sí registran uno.
ALTER TABLE comercial.clientes
    ALTER COLUMN telefono DROP NOT NULL;

COMMENT ON COLUMN comercial.clientes.telefono IS
    'Teléfono del cliente. Opcional (NULL permitido): PostgreSQL admite múltiples NULL manteniendo el UNIQUE. No se generan placeholders ni teléfonos falsos.';
