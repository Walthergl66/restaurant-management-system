-- =====================================================================
-- Cuenta del cliente en la app móvil (RF-45).
--
-- Tres piezas del mismo flujo de cuenta, en una sola migración para que
-- la carpeta no crezca con un archivo por cada ajuste:
--
--  - Recuperación de contraseña: token de un solo uso guardado hasheado
--    (SHA-256) con expiración. No se guarda el token en claro: si se filtra
--    la base no sirve.
--
--  - Métodos de pago guardados: solo metadata no sensible (tipo, alias y
--    últimos 4 dígitos). NUNCA se guarda el número completo de la tarjeta
--    (PAN) ni el CVV.
--
--  - Verificación de correo: el correo es el nombre de usuario (login).
--    Hasta que el cliente confirma el código recibido, la cuenta queda sin
--    verificar y no puede iniciar sesión. El código se guarda hasheado
--    (SHA-256), con expiración y de un solo uso, igual que el token de
--    recuperación. Los usuarios preexistentes y los creados por
--    administración quedan verificados por defecto (DEFAULT TRUE).
-- =====================================================================

-- ---------------------------------------------------------------
-- Recuperación de contraseña
-- ---------------------------------------------------------------
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

-- ---------------------------------------------------------------
-- Métodos de pago guardados del cliente
-- ---------------------------------------------------------------
CREATE TABLE comercial.metodos_pago_cliente (
    id             BIGSERIAL PRIMARY KEY,
    cliente_id     BIGINT      NOT NULL REFERENCES comercial.clientes (id),
    tipo           VARCHAR(20) NOT NULL
                   CHECK (tipo IN ('EFECTIVO', 'TARJETA', 'TRANSFERENCIA', 'OTRO')),
    alias          VARCHAR(40) NOT NULL,
    ultimos4       VARCHAR(4),
    predeterminado BOOLEAN     NOT NULL DEFAULT FALSE,
    activo         BOOLEAN     NOT NULL DEFAULT TRUE,
    creado_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    actualizado_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_metodos_pago_cliente ON comercial.metodos_pago_cliente (cliente_id);

-- ---------------------------------------------------------------
-- Verificación de correo
-- ---------------------------------------------------------------
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
