-- V8: Auditoría (Fase 7 del plan: RF-52, RNF-10, RNF-11).
--
-- Historial de operaciones del sistema. Tabla de SOLO INSERCIÓN (RNF-10):
-- el usuario de la aplicación nunca actualiza ni borra nada aquí; se revoca
-- UPDATE/DELETE. En dev/test el rol de conexión es superusuario (ignora la
-- REVOKE), así que la defensa se condiciona a que el rol existe y no es
-- superuser — la garantía real es de diseño: la entidad solo modela INSERT.

CREATE TABLE auditoria_eventos (
    id         BIGSERIAL PRIMARY KEY,
    usuario    VARCHAR(50)  NOT NULL,
    tipo       VARCHAR(40)  NOT NULL,
    entidad    VARCHAR(40)  NOT NULL,
    entidad_id VARCHAR(50),
    detalle    JSONB,
    fecha      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_auditoria_entidad ON auditoria_eventos (entidad, entidad_id);
CREATE INDEX idx_auditoria_fecha   ON auditoria_eventos (fecha);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'restaurante'
               AND NOT rolsuper) THEN
        EXECUTE 'REVOKE UPDATE, DELETE ON auditoria_eventos FROM restaurante';
    END IF;
END $$;