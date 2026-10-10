-- =====================================================================
-- V9: recuperación de contraseña (RF-45).
-- Tokens de un solo uso, guardados hasheados (SHA-256), con expiración.
-- No se guarda el token en claro: si se filtra la base no sirve.
-- =====================================================================

CREATE TABLE seguridad.recuperacion_password (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT      NOT NULL REFERENCES seguridad.usuarios (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expira_at  TIMESTAMPTZ NOT NULL,
    usado      BOOLEAN     NOT NULL DEFAULT FALSE,
    creado_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recuperacion_password_usuario
    ON seguridad.recuperacion_password (usuario_id);
