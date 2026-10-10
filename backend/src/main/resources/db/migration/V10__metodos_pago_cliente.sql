-- =====================================================================
-- V10: métodos de pago guardados del cliente (RF-45).
-- Solo metadata no sensible: tipo, alias y últimos 4 dígitos. NUNCA se
-- guarda el número completo de la tarjeta (PAN) ni el CVV.
-- =====================================================================

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
