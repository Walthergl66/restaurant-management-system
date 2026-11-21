-- =====================================================================
-- Zonas del restaurante (Salón, Terraza, Patio…).
--
-- Hasta ahora cada mesa guardaba su ubicación como texto libre
-- (catalogo.mesas.ubicacion, por defecto 'SALON'). Un error de tipeo creaba
-- una "zona" nueva y no había orden ni gestión. Esta migración:
--
--   1. Crea catalogo.zonas (nombre único sin distinguir mayúsculas, orden).
--   2. Crea una zona por cada ubicación distinta existente, unificando
--      mayúsculas, espacios y tildes ('SALON', 'salon' y 'Salón ' son una sola).
--   3. Enlaza cada mesa con su zona (zona_id) y elimina la columna ubicacion.
--
-- ANTES DE APLICARLA en una base con datos: hacer respaldo (database/backup.sh).
-- La columna ubicacion se elimina; su información queda en las zonas.
-- =====================================================================

CREATE TABLE catalogo.zonas (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(60) NOT NULL,
    orden       INT         NOT NULL CHECK (orden >= 1),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

-- El nombre es único sin distinguir mayúsculas. Las tildes las unifica la aplicación.
CREATE UNIQUE INDEX uq_zonas_nombre ON catalogo.zonas (lower(nombre));

-- Una zona por cada ubicación distinta (clave sin mayúsculas, espacios dobles ni tildes).
-- 'Salón' va primero; el resto, por antigüedad de la primera mesa que la usó.
WITH normalizadas AS (
    SELECT id,
           regexp_replace(trim(ubicacion), '\s+', ' ', 'g') AS texto,
           translate(lower(regexp_replace(trim(ubicacion), '\s+', ' ', 'g')), 'áéíóúü', 'aeiouu') AS clave
    FROM catalogo.mesas
    WHERE trim(ubicacion) <> ''
), primeras AS (
    SELECT DISTINCT ON (clave) clave, texto, id
    FROM normalizadas
    ORDER BY clave, id
)
INSERT INTO catalogo.zonas (nombre, orden)
SELECT CASE WHEN clave = 'salon' THEN 'Salón'
            ELSE left(upper(left(texto, 1)) || substr(texto, 2), 60) END,
       row_number() OVER (ORDER BY CASE WHEN clave = 'salon' THEN 0 ELSE 1 END, id)
FROM primeras;

-- Debe existir al menos una zona (base sin mesas, o solo con ubicaciones vacías).
INSERT INTO catalogo.zonas (nombre, orden)
SELECT 'Salón', 1
WHERE NOT EXISTS (SELECT 1 FROM catalogo.zonas);

ALTER TABLE catalogo.mesas ADD COLUMN zona_id BIGINT REFERENCES catalogo.zonas (id);

-- Cada mesa a la zona de su ubicación (la vacía va a 'Salón').
UPDATE catalogo.mesas m
SET zona_id = z.id
FROM catalogo.zonas z
WHERE translate(lower(z.nombre), 'áéíóúü', 'aeiouu') =
      CASE WHEN trim(m.ubicacion) = '' THEN 'salon'
           ELSE translate(lower(regexp_replace(trim(m.ubicacion), '\s+', ' ', 'g')), 'áéíóúü', 'aeiouu')
      END;

-- Red de seguridad: lo que no hubiera coincidido (p. ej. nombres truncados a 60) va a la primera zona.
UPDATE catalogo.mesas
SET zona_id = (SELECT id FROM catalogo.zonas ORDER BY orden, id LIMIT 1)
WHERE zona_id IS NULL;

ALTER TABLE catalogo.mesas ALTER COLUMN zona_id SET NOT NULL;
ALTER TABLE catalogo.mesas DROP COLUMN ubicacion;

CREATE INDEX idx_mesas_zona ON catalogo.mesas (zona_id);
