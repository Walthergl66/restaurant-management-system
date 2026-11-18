-- =====================================================================
-- Coordenadas (lat/lng) en las direcciones de domicilio del cliente.
-- =====================================================================
-- El checkout a domicilio permite confirmar la ubicación exacta con un
-- marcador en el mapa. Se guardan tanto la descripción textual (columna
-- direccion, ya existente) como las coordenadas confirmadas por el usuario.
--
-- Nullable: las direcciones creadas antes de esta función no tienen
-- coordenadas y siguen siendo válidas (compatibilidad). NUMERIC(10,7) da
-- 7 decimales, la misma precisión que usa la caché de geocoding de delivery.
ALTER TABLE comercial.clientes_direcciones
    ADD COLUMN lat NUMERIC(10, 7),
    ADD COLUMN lng NUMERIC(10, 7);

COMMENT ON COLUMN comercial.clientes_direcciones.lat IS
    'Latitud confirmada del marcador en el checkout (grados decimales). NULL en direcciones antiguas.';
COMMENT ON COLUMN comercial.clientes_direcciones.lng IS
    'Longitud confirmada del marcador en el checkout (grados decimales). NULL en direcciones antiguas.';
