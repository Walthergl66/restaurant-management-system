-- =====================================================================
-- V11: verificación de correo del cliente (RF-45).
-- El correo es el nombre de usuario (login). Hasta que el cliente confirma
-- el código enviado a su correo, la cuenta queda sin verificar y no puede
-- iniciar sesión. El código se guarda hasheado (SHA-256), con expiración y
-- de un solo uso, igual que el token de recuperación de contraseña.
-- Los usuarios preexistentes y los creados por administración quedan
-- verificados por defecto (DEFAULT TRUE).
-- =====================================================================

ALTER TABLE seguridad.usuarios
    ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE seguridad.verificacion_email (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL REFERENCES seguridad.usuarios (id) ON DELETE CASCADE,
    codigo_hash VARCHAR(64) NOT NULL,
    expira_at   TIMESTAMPTZ NOT NULL,
    usado       BOOLEAN     NOT NULL DEFAULT FALSE,
    creado_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_verificacion_email_usuario
    ON seguridad.verificacion_email (usuario_id);
