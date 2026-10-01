-- =====================================================================
-- V14__outbox_version.sql. ADITIVA sobre V1..V13.
-- Versión optimista del evento del outbox: el agente de impresión y el
-- difusor de estados pueden acknowledgementar el mismo evento a la vez. Sin
-- versión, la segunda escritura pisa a la primera sin avisar y `intentos`
-- se pierde, con lo que un FALLIDO puede pasar a ENVIADO sin reintento.
ALTER TABLE outbox ADD COLUMN version BIGINT NOT NULL DEFAULT 0;