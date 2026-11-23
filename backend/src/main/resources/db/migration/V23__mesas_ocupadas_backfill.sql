-- =====================================================================
-- Estado real de las mesas.
--
-- Hasta ahora catalogo.mesas.estado se quedaba siempre en 'LIBRE'. Desde
-- este cambio el backend la marca OCUPADA al abrirse su cuenta y LIBRE al
-- cerrarse (cobro, cierre manual o último borrador cancelado). Esta
-- migración alinea las mesas que YA tienen una cuenta abierta, para que no
-- aparezcan libres hasta su próximo pedido.
--
-- Idempotente: puede reaplicarse sin cambiar el resultado.
-- =====================================================================

UPDATE catalogo.mesas
SET estado = 'OCUPADA'
WHERE estado = 'LIBRE'
  AND id IN (SELECT mesa_id FROM operaciones.cuentas WHERE estado = 'ABIERTA');
