-- =====================================================================
-- V11__comanda_version.sql. ADITIVA sobre V1..V10.
-- Versión optimista de la comanda: dos terminals de cocina pueden mover la
-- misma comanda a la vez (uno la da por lista mientras el otro la vuelve a
-- tocar). Sin versión, la segunda escritura pisa a la primera sin avisar y
-- la comanda retrocede de LISTO a EN_PREPARACION. Con la columna, la
-- escritura que llega tarde se rechaza con un 409 en vez de perder el plato.
ALTER TABLE comandas ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
