-- =====================================================================
-- Celular de contacto del usuario.
--
-- El auto-registro de repartidor (rol DELIVERY) captura el celular con el
-- que el restaurante lo contacta. La columna es opcional: solo la usan las
-- cuentas que se auto-registran (hoy, los repartidores); el resto de las
-- cuentas se contactan por correo.
--
-- No es única: el correo (username) ya impide dos cuentas iguales y dos
-- repartidores pueden compartir un número (turnos, líneas compartidas).
-- =====================================================================
ALTER TABLE seguridad.usuarios
    ADD COLUMN IF NOT EXISTS celular VARCHAR(20);
