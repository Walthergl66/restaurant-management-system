-- =====================================================================
-- V12__mesa_version.sql. ADITIVA sobre V1..V11.
-- Versión optimista de la mesa: su estado se muta desde cuatro rutas
-- (PATCH /estado, ocupar al crear pedido, liberar al cancelar el último
-- borrador y liberar al cobrar). Sin versión, dos escrituras concurrentes
-- se pisan: el cajero libera la mesa mientras el mesero la ocupa y queda
-- LIBRE con un pedido confirmado encima. Con la columna, la escritura
-- tardía se rechaza con un 409 en vez de perder el estado.
ALTER TABLE mesas ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
