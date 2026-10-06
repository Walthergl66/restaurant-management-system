-- =====================================================================
-- Schema: SEGURIDAD
-- Responsabilidad: identidad y autorización de los usuarios del sistema.
-- Aquí vive quién es cada usuario (usuarios, refresh_tokens para sesiones)
-- y qué puede hacer (roles, permisos, roles_permisos). Se aísla porque es
-- el perímetro de acceso: cualquier falla o consulta errante en este schema
-- afecta la autenticación de TODO el sistema, no solo un módulo de negocio.
--
-- Tablas: roles, permisos, roles_permisos, usuarios, refresh_tokens.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS seguridad;

CREATE TABLE seguridad.roles (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(30)  NOT NULL UNIQUE,
    descripcion VARCHAR(120),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE seguridad.permisos (
    id          BIGSERIAL PRIMARY KEY,
    codigo      VARCHAR(60)  NOT NULL UNIQUE,
    modulo      VARCHAR(30)  NOT NULL,
    descripcion VARCHAR(120),
    activo      BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE seguridad.roles_permisos (
    rol_id     BIGINT NOT NULL REFERENCES seguridad.roles (id) ON DELETE CASCADE,
    permiso_id BIGINT NOT NULL REFERENCES seguridad.permisos (id) ON DELETE CASCADE,
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE TABLE seguridad.usuarios (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(100) NOT NULL,
    nombre          VARCHAR(100) NOT NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    rol_id          BIGINT       NOT NULL REFERENCES seguridad.roles (id),
    sesion_version  BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50)
);

CREATE TABLE seguridad.refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT      NOT NULL REFERENCES seguridad.usuarios (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_usuarios_rol ON seguridad.usuarios (rol_id);
CREATE INDEX idx_refresh_tokens_usuario ON seguridad.refresh_tokens (usuario_id);
