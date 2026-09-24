-- =====================================================================
-- V10__sesion_version.sql  Auditoría A-04. ADITIVA sobre V1..V9.
-- Versión de credenciales/sesión por usuario para revocación inmediata:
-- al cambiar la contraseña, el rol o el estado (activar/desactivar) se
-- incrementa, de modo que los JWT emitidos antes quedan inválidos (el
-- filtro compara la versión embebida con la actual en la base) y los
-- refresh tokens activos se revocan en el servicio.
ALTER TABLE usuarios ADD COLUMN sesion_version BIGINT NOT NULL DEFAULT 0;