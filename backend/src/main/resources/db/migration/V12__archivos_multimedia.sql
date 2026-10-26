CREATE SCHEMA archivos;
CREATE TABLE archivos.archivos (
    id UUID PRIMARY KEY,
    nombre_original VARCHAR(255) NOT NULL,
    clave VARCHAR(100) NOT NULL UNIQUE,
    tipo VARCHAR(10) NOT NULL CHECK (tipo IN ('IMAGEN', 'VIDEO')),
    contenido_tipo VARCHAR(100) NOT NULL,
    tamano BIGINT NOT NULL CHECK (tamano > 0),
    propietario VARCHAR(255) NOT NULL,
    producto_id BIGINT,
    estado VARCHAR(12) NOT NULL CHECK (estado IN ('PENDIENTE', 'VINCULADO', 'RETIRADO')),
    creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    modificado_en TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX archivos_limpieza_idx ON archivos.archivos (estado, modificado_en);
ALTER TABLE catalogo.productos ADD COLUMN imagen_archivo_id UUID REFERENCES archivos.archivos(id);
ALTER TABLE catalogo.productos ADD COLUMN video_archivo_id UUID REFERENCES archivos.archivos(id);
ALTER TABLE catalogo.productos ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
