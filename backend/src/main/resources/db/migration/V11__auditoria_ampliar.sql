-- Ampliar auditoría con categoría, IP, user-agent y requestId
ALTER TABLE auditoria.auditoria_eventos
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(32),
    ADD COLUMN IF NOT EXISTS ip INET,
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(255),
    ADD COLUMN IF NOT EXISTS request_id VARCHAR(36);

UPDATE auditoria.auditoria_eventos SET categoria = 'NEGOCIO' WHERE categoria IS NULL;
ALTER TABLE auditoria.auditoria_eventos ALTER COLUMN categoria SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_auditoria_categoria_fecha ON auditoria.auditoria_eventos(categoria, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_usuario_fecha ON auditoria.auditoria_eventos(usuario, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_tipo_fecha ON auditoria.auditoria_eventos(tipo, fecha DESC);
CREATE INDEX IF NOT EXISTS idx_auditoria_ip_fecha ON auditoria.auditoria_eventos(ip, fecha DESC);

-- Auditoría solo-inserción: revocar UPDATE/DELETE al usuario de la app
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'restaurante') THEN
        EXECUTE 'REVOKE UPDATE, DELETE ON auditoria.auditoria_eventos FROM restaurante';
        EXECUTE 'GRANT INSERT, SELECT ON auditoria.auditoria_eventos TO restaurante';
        EXECUTE 'GRANT USAGE ON ALL SEQUENCES IN SCHEMA auditoria TO restaurante';
    END IF;
END
$$;
