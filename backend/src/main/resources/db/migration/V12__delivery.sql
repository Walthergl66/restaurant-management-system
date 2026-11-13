-- =====================================================================
-- Schema: DELIVERY
-- Responsabilidad: el reparto a domicilio de los pedidos del cliente.
-- Es un canal LOGÍSTICO nuevo, separado del estado de preparación del
-- pedido (comercial.pedidos_clientes.estado) y del rol del usuario
-- (seguridad): el estado operativo del repartidor vive aquí, no en el rol.
--
-- Reglas de frontera que justifican este schema propio:
--  - El rol DELIVERY (schema seguridad) dice QUIÉN puede ser repartidor;
--    delivery.repartidores.estado dice si ese repartidor OPERA hoy
--    (PENDIENTE/HABILITADO/SUSPENDIDO). Son cosas distintas a propósito.
--  - Los estados de reparto (asignación/recorrido/parada) NUNCA tocan ni
--    sobrescriben comercial.pedidos_clientes.estado: la preparación del
--    pedido y su reparto son líneas de tiempo independientes.
--  - comercial.clientes_direcciones NO guarda coordenadas; el geocoding
--    se cachea aquí por direccion_id para no regeocodificar.
--
-- Tablas: repartidores, recorridos, asignaciones, paradas, incidencias,
--         geocoding_cache, rutas_cache.
-- Orden: recorridos antes que asignaciones (asignaciones.recorrido_id la
-- referencia), y asignaciones antes que paradas (paradas.asignacion_id).
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS delivery;

-- ---------------------------------------------------------------
-- Repartidores: estado operativo SEPARADO del rol de seguridad.
-- Un repartidor PENDIENTE no puede recibir asignaciones; solo el
-- HABILITADO opera. SUSPENDIDO lo deja fuera de ruta sin borrarlo.
-- Vínculo 1:1 con el usuario logueado (rol DELIVERY vive en seguridad).
-- ---------------------------------------------------------------
CREATE TABLE delivery.repartidores (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT      NOT NULL UNIQUE REFERENCES seguridad.usuarios (id),
    estado         VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                   CHECK (estado IN ('PENDIENTE', 'HABILITADO', 'SUSPENDIDO')),
    habilitado_por VARCHAR(50),
    habilitado_at  TIMESTAMPTZ,
    version        BIGINT      NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50)
);

-- ---------------------------------------------------------------
-- Recorridos: una ruta de un repartidor, INDIVIDUAL (un pedido) o
-- MULTIPLE (varias paradas optimizadas por ORS en fases posteriores).
-- Guarda el criterio de optimización pedido (distancia/duración) y los
-- totales calculados. Va antes que asignaciones por la FK recorrido_id.
-- ---------------------------------------------------------------
CREATE TABLE delivery.recorridos (
    id                     BIGSERIAL PRIMARY KEY,
    repartidor_id          BIGINT      NOT NULL REFERENCES delivery.repartidores (id),
    tipo                   VARCHAR(20) NOT NULL
                           CHECK (tipo IN ('INDIVIDUAL', 'MULTIPLE')),
    estado                 VARCHAR(20) NOT NULL DEFAULT 'PLANIFICADO'
                           CHECK (estado IN ('PLANIFICADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO')),
    criterio_optimizacion  VARCHAR(20) NOT NULL DEFAULT 'DURACION'
                           CHECK (criterio_optimizacion IN ('DISTANCIA', 'DURACION')),
    distancia_total_m      INTEGER,
    duracion_total_s       INTEGER,
    iniciado_at            TIMESTAMPTZ,
    finalizado_at          TIMESTAMPTZ,
    version                BIGINT      NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by             VARCHAR(50),
    updated_by             VARCHAR(50)
);

CREATE INDEX idx_recorridos_repartidor ON delivery.recorridos (repartidor_id);

-- ---------------------------------------------------------------
-- Asignaciones: un pedido de cliente asignado a un repartidor. El estado
-- es LOGÍSTICO (ASIGNADA/EN_RUTA/ENTREGADA/FALLIDA/REASIGNADA) y no toca
-- el estado de preparación del pedido en comercial. recorrido_id es
-- nullable: una asignación nace suelta y luego entra a un recorrido.
--
-- Unicidad: un pedido solo puede tener UNA asignación ACTIVA a la vez.
-- REASIGNADA y FALLIDA son terminales/liberadoras, así que quedan fuera
-- del índice único parcial y permiten reasignar el pedido.
-- ---------------------------------------------------------------
CREATE TABLE delivery.asignaciones (
    id                BIGSERIAL PRIMARY KEY,
    pedido_cliente_id BIGINT      NOT NULL REFERENCES comercial.pedidos_clientes (id),
    repartidor_id     BIGINT      NOT NULL REFERENCES delivery.repartidores (id),
    recorrido_id      BIGINT      REFERENCES delivery.recorridos (id),
    estado            VARCHAR(20) NOT NULL DEFAULT 'ASIGNADA'
                      CHECK (estado IN ('ASIGNADA', 'EN_RUTA', 'ENTREGADA', 'FALLIDA', 'REASIGNADA')),
    asignado_por      VARCHAR(50),
    asignado_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT      NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by        VARCHAR(50),
    updated_by        VARCHAR(50)
);

-- Un pedido solo admite una asignación viva: las terminales liberadoras
-- (REASIGNADA, FALLIDA) no cuentan, de modo que se pueda reasignar.
CREATE UNIQUE INDEX uq_asignacion_activa_por_pedido
    ON delivery.asignaciones (pedido_cliente_id)
    WHERE estado NOT IN ('REASIGNADA', 'FALLIDA');

CREATE INDEX idx_asignaciones_repartidor ON delivery.asignaciones (repartidor_id);
CREATE INDEX idx_asignaciones_recorrido  ON delivery.asignaciones (recorrido_id);

-- ---------------------------------------------------------------
-- Paradas: las entregas ORDENADAS dentro de un recorrido. El `orden`
-- es 0-based (la primera parada es 0) y lo decide ORS Optimization en
-- fases posteriores; aquí solo se persiste. Cada parada apunta a la
-- asignación (el pedido) que resuelve.
--
-- UNIQUE(recorrido_id, asignacion_id): un mismo pedido no se repite dos
-- veces en el mismo recorrido.
-- ---------------------------------------------------------------
CREATE TABLE delivery.paradas (
    id            BIGSERIAL PRIMARY KEY,
    recorrido_id  BIGINT      NOT NULL REFERENCES delivery.recorridos (id),
    asignacion_id BIGINT      NOT NULL REFERENCES delivery.asignaciones (id),
    orden         INTEGER     NOT NULL,
    estado        VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
                  CHECK (estado IN ('PENDIENTE', 'ENTREGADA', 'FALLIDA')),
    entregado_at  TIMESTAMPTZ,
    version       BIGINT      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by    VARCHAR(50),
    updated_by    VARCHAR(50),
    CONSTRAINT uq_parada_recorrido_asignacion UNIQUE (recorrido_id, asignacion_id)
);

CREATE INDEX idx_paradas_recorrido ON delivery.paradas (recorrido_id);

-- ---------------------------------------------------------------
-- Incidencias: entregas fallidas con su motivo. Solo INSERCIÓN: el
-- registro se conserva para trazabilidad, nunca se edita ni se borra.
-- ---------------------------------------------------------------
CREATE TABLE delivery.incidencias (
    id             BIGSERIAL PRIMARY KEY,
    parada_id      BIGINT       NOT NULL REFERENCES delivery.paradas (id),
    motivo         VARCHAR(300) NOT NULL,
    registrado_por VARCHAR(50),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_incidencias_parada ON delivery.incidencias (parada_id);

-- ---------------------------------------------------------------
-- Caché de geocoding: coordenadas calculadas para una dirección, para
-- no regeocodificar y no gastar la cuota gratuita de ORS. Única por
-- direccion_id. Se guarda el texto de la dirección para invalidar la
-- caché si la dirección original cambia.
-- ---------------------------------------------------------------
CREATE TABLE delivery.geocoding_cache (
    id               BIGSERIAL PRIMARY KEY,
    direccion_id     BIGINT        NOT NULL UNIQUE REFERENCES comercial.clientes_direcciones (id),
    lat              NUMERIC(10,7) NOT NULL,
    lng              NUMERIC(10,7) NOT NULL,
    direccion_texto  VARCHAR(200),
    geocodificado_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------
-- Caché de rutas: geometría y resumen devueltos por ORS Directions,
-- reutilizables mientras sigan válidos. clave_hash identifica el
-- conjunto origen+paradas ordenadas para reusar el cálculo. recorrido_id
-- es nullable para permitir rutas puntuales no ligadas a un recorrido.
-- La reutilización real se implementa en las fases ORS; aquí solo el
-- esquema.
-- ---------------------------------------------------------------
CREATE TABLE delivery.rutas_cache (
    id           BIGSERIAL PRIMARY KEY,
    recorrido_id BIGINT      REFERENCES delivery.recorridos (id),
    clave_hash   VARCHAR(64) NOT NULL,
    geometria    TEXT,
    distancia_m  INTEGER,
    duracion_s   INTEGER,
    calculado_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_rutas_cache_clave ON delivery.rutas_cache (clave_hash);
