-- =====================================================================
-- Schema: DELIVERY — Configuración de la ubicación del restaurante.
--
-- Responsabilidad: la ubicación operativa del restaurante es el ORIGEN de
-- todas las rutas de reparto. Hasta ahora salía de application.yml
-- (app.delivery.origen / DELIVERY_ORIGEN_LAT/LNG); el requisito nuevo es
-- que el administrador la EDITE desde el panel y quede PERSISTIDA. Por eso
-- vive en una tabla propia del schema delivery, no en env vars.
--
-- Fila única: el restaurante tiene una sola ubicación. Se fuerza con un
-- índice único sobre una columna constante (fila_unica), de modo que no
-- puedan existir dos configuraciones a la vez. El registro se siembra aquí
-- con la dirección PROVISIONAL y SIN coordenadas: estas se calculan por
-- geocoding (ORS) o las fija el administrador marcando el punto en el mapa.
-- No se inventan coordenadas en el SQL.
--
-- origen_coords indica de dónde salieron las coordenadas vigentes:
--   GEOCODING -> resueltas por ORS a partir de la dirección.
--   MANUAL    -> el administrador las marcó en el mapa.
--   NULL      -> aún no hay coordenadas (pendiente de selección manual).
-- =====================================================================

-- ---------------------------------------------------------------
-- Tabla de configuración del restaurante (fila única).
-- Columnas de auditoría iguales al resto de tablas delivery (V12).
-- ---------------------------------------------------------------
CREATE TABLE delivery.config_restaurante (
    id              BIGSERIAL PRIMARY KEY,
    direccion       VARCHAR(200)  NOT NULL,
    lat             NUMERIC(10,7),
    lng             NUMERIC(10,7),
    geocodificado_at TIMESTAMPTZ,
    origen_coords   VARCHAR(20)
                    CHECK (origen_coords IN ('GEOCODING', 'MANUAL')),
    -- Columna constante que garantiza una sola fila vía índice único.
    fila_unica      BOOLEAN       NOT NULL DEFAULT TRUE,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by      VARCHAR(50),
    updated_by      VARCHAR(50)
);

-- Un solo registro de configuración del restaurante.
CREATE UNIQUE INDEX uq_config_restaurante_fila_unica
    ON delivery.config_restaurante (fila_unica);

-- ---------------------------------------------------------------
-- Semilla de la dirección PROVISIONAL, SIN coordenadas. Las coords se
-- calculan por geocoding (ORS) o las fija el administrador en el mapa.
-- ---------------------------------------------------------------
INSERT INTO delivery.config_restaurante (direccion, lat, lng, origen_coords)
VALUES ('5 de Junio, Jaramijo, Manabi, Ecuador', NULL, NULL, NULL);

-- ---------------------------------------------------------------
-- Permiso del panel admin para gestionar la configuración del
-- restaurante. Mismo patrón que V13: alta idempotente del permiso y
-- asignación explícita al rol ADMIN (ambos ON CONFLICT DO NOTHING).
-- ---------------------------------------------------------------
INSERT INTO seguridad.permisos (codigo, modulo, descripcion) VALUES
 ('delivery:config-gestionar', 'delivery', 'Admin: configurar la ubicacion del restaurante (origen de rutas)')
ON CONFLICT (codigo) DO NOTHING;

SELECT setval('seguridad.permisos_id_seq', (SELECT MAX(id) FROM seguridad.permisos));

INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM seguridad.roles r
JOIN seguridad.permisos p ON p.codigo = 'delivery:config-gestionar'
WHERE r.codigo = 'ADMIN'
ON CONFLICT DO NOTHING;
