-- Esquema generado con pg_dump --schema-only tras la migración V23 (2026-11-21).
-- Referencia de lectura: la fuente de verdad son las migraciones de backend/src/main/resources/db/migration.
-- No editar a mano; se regenera con cada migración nueva (ver database/README.md).
-- Solo estructura: no incluye los datos semilla (roles, permisos, parámetros); esos los carga Flyway (V8, V10, V13, V22...).

--
-- PostgreSQL database dump
--


-- Dumped from database version 17.12 (Debian 17.12-1.pgdg13+1)
-- Dumped by pg_dump version 17.12 (Debian 17.12-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: archivos; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA archivos;


--
-- Name: auditoria; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA auditoria;


--
-- Name: catalogo; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA catalogo;


--
-- Name: comercial; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA comercial;


--
-- Name: configuracion; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA configuracion;


--
-- Name: delivery; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA delivery;


--
-- Name: operaciones; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA operaciones;


--
-- Name: seguridad; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA seguridad;


--
-- Name: tesoreria; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA tesoreria;


--
-- Name: fn_admin_todos_los_permisos(); Type: FUNCTION; Schema: seguridad; Owner: -
--

CREATE FUNCTION seguridad.fn_admin_todos_los_permisos() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.activo THEN
        INSERT INTO seguridad.roles_permisos (rol_id, permiso_id)
        SELECT r.id, NEW.id
        FROM seguridad.roles r
        WHERE r.codigo = 'ADMIN'
        ON CONFLICT DO NOTHING;
    ELSE
        DELETE FROM seguridad.roles_permisos rp
        USING seguridad.roles r
        WHERE rp.rol_id = r.id
          AND r.codigo = 'ADMIN'
          AND rp.permiso_id = NEW.id;
    END IF;
    RETURN NEW;
END;
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: archivos; Type: TABLE; Schema: archivos; Owner: -
--

CREATE TABLE archivos.archivos (
    id uuid NOT NULL,
    nombre_original character varying(255) NOT NULL,
    clave character varying(100) NOT NULL,
    tipo character varying(10) NOT NULL,
    contenido_tipo character varying(100) NOT NULL,
    tamano bigint NOT NULL,
    propietario character varying(255) NOT NULL,
    producto_id bigint,
    estado character varying(12) NOT NULL,
    creado_en timestamp with time zone NOT NULL,
    modificado_en timestamp with time zone NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT archivos_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'VINCULADO'::character varying, 'RETIRADO'::character varying])::text[]))),
    CONSTRAINT archivos_tamano_check CHECK ((tamano > 0)),
    CONSTRAINT archivos_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['IMAGEN'::character varying, 'VIDEO'::character varying])::text[])))
);


--
-- Name: auditoria_eventos; Type: TABLE; Schema: auditoria; Owner: -
--

CREATE TABLE auditoria.auditoria_eventos (
    id bigint NOT NULL,
    usuario character varying(50) NOT NULL,
    tipo character varying(40) NOT NULL,
    entidad character varying(40) NOT NULL,
    entidad_id character varying(50),
    detalle jsonb,
    fecha timestamp with time zone DEFAULT now() NOT NULL,
    categoria character varying(32) NOT NULL,
    ip inet,
    user_agent character varying(255),
    request_id character varying(36)
);


--
-- Name: auditoria_eventos_id_seq; Type: SEQUENCE; Schema: auditoria; Owner: -
--

CREATE SEQUENCE auditoria.auditoria_eventos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: auditoria_eventos_id_seq; Type: SEQUENCE OWNED BY; Schema: auditoria; Owner: -
--

ALTER SEQUENCE auditoria.auditoria_eventos_id_seq OWNED BY auditoria.auditoria_eventos.id;


--
-- Name: outbox; Type: TABLE; Schema: auditoria; Owner: -
--

CREATE TABLE auditoria.outbox (
    id bigint NOT NULL,
    tipo character varying(40) NOT NULL,
    agregado_id character varying(64) NOT NULL,
    pedido_codigo character varying(40),
    numero_comanda integer,
    area_id bigint,
    area_nombre character varying(80),
    evento character varying(80) NOT NULL,
    estado character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    intentos integer DEFAULT 0 NOT NULL,
    error character varying(500),
    creada_at timestamp with time zone DEFAULT now() NOT NULL,
    procesada_at timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT outbox_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ENVIADO'::character varying, 'FALLIDO'::character varying])::text[])))
);


--
-- Name: outbox_id_seq; Type: SEQUENCE; Schema: auditoria; Owner: -
--

CREATE SEQUENCE auditoria.outbox_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: outbox_id_seq; Type: SEQUENCE OWNED BY; Schema: auditoria; Owner: -
--

ALTER SEQUENCE auditoria.outbox_id_seq OWNED BY auditoria.outbox.id;


--
-- Name: areas; Type: TABLE; Schema: catalogo; Owner: -
--

CREATE TABLE catalogo.areas (
    id bigint NOT NULL,
    nombre character varying(80) NOT NULL,
    descripcion character varying(200),
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50)
);


--
-- Name: areas_id_seq; Type: SEQUENCE; Schema: catalogo; Owner: -
--

CREATE SEQUENCE catalogo.areas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: areas_id_seq; Type: SEQUENCE OWNED BY; Schema: catalogo; Owner: -
--

ALTER SEQUENCE catalogo.areas_id_seq OWNED BY catalogo.areas.id;


--
-- Name: categorias; Type: TABLE; Schema: catalogo; Owner: -
--

CREATE TABLE catalogo.categorias (
    id bigint NOT NULL,
    nombre character varying(80) NOT NULL,
    descripcion character varying(200),
    orden integer DEFAULT 0 NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50)
);


--
-- Name: categorias_id_seq; Type: SEQUENCE; Schema: catalogo; Owner: -
--

CREATE SEQUENCE catalogo.categorias_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: categorias_id_seq; Type: SEQUENCE OWNED BY; Schema: catalogo; Owner: -
--

ALTER SEQUENCE catalogo.categorias_id_seq OWNED BY catalogo.categorias.id;


--
-- Name: extras; Type: TABLE; Schema: catalogo; Owner: -
--

CREATE TABLE catalogo.extras (
    id bigint NOT NULL,
    nombre character varying(80) NOT NULL,
    descripcion character varying(200),
    precio numeric(12,2) DEFAULT 0 NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT extras_precio_check CHECK ((precio >= (0)::numeric))
);


--
-- Name: extras_id_seq; Type: SEQUENCE; Schema: catalogo; Owner: -
--

CREATE SEQUENCE catalogo.extras_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: extras_id_seq; Type: SEQUENCE OWNED BY; Schema: catalogo; Owner: -
--

ALTER SEQUENCE catalogo.extras_id_seq OWNED BY catalogo.extras.id;


--
-- Name: mesas; Type: TABLE; Schema: catalogo; Owner: -
--

CREATE TABLE catalogo.mesas (
    id bigint NOT NULL,
    numero integer NOT NULL,
    capacidad integer NOT NULL,
    estado character varying(20) DEFAULT 'LIBRE'::character varying NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    zona_id bigint NOT NULL,
    CONSTRAINT mesas_capacidad_check CHECK (((capacidad >= 1) AND (capacidad <= 50))),
    CONSTRAINT mesas_estado_check CHECK (((estado)::text = ANY ((ARRAY['LIBRE'::character varying, 'OCUPADA'::character varying, 'RESERVADA'::character varying, 'INACTIVA'::character varying])::text[])))
);


--
-- Name: mesas_id_seq; Type: SEQUENCE; Schema: catalogo; Owner: -
--

CREATE SEQUENCE catalogo.mesas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: mesas_id_seq; Type: SEQUENCE OWNED BY; Schema: catalogo; Owner: -
--

ALTER SEQUENCE catalogo.mesas_id_seq OWNED BY catalogo.mesas.id;


--
-- Name: producto_extras; Type: TABLE; Schema: catalogo; Owner: -
--

CREATE TABLE catalogo.producto_extras (
    producto_id bigint NOT NULL,
    extra_id bigint NOT NULL
);


--
-- Name: producto_ingredientes; Type: TABLE; Schema: catalogo; Owner: -
--

CREATE TABLE catalogo.producto_ingredientes (
    id bigint NOT NULL,
    producto_id bigint NOT NULL,
    nombre character varying(80) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50)
);


--
-- Name: producto_ingredientes_id_seq; Type: SEQUENCE; Schema: catalogo; Owner: -
--

CREATE SEQUENCE catalogo.producto_ingredientes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: producto_ingredientes_id_seq; Type: SEQUENCE OWNED BY; Schema: catalogo; Owner: -
--

ALTER SEQUENCE catalogo.producto_ingredientes_id_seq OWNED BY catalogo.producto_ingredientes.id;


--
-- Name: productos; Type: TABLE; Schema: catalogo; Owner: -
--

CREATE TABLE catalogo.productos (
    id bigint NOT NULL,
    nombre character varying(120) NOT NULL,
    descripcion character varying(500),
    imagen_url character varying(300),
    precio numeric(12,2) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    categoria_id bigint,
    area_id bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    imagen_archivo_id uuid,
    video_archivo_id uuid,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT productos_precio_check CHECK ((precio >= (0)::numeric))
);


--
-- Name: productos_id_seq; Type: SEQUENCE; Schema: catalogo; Owner: -
--

CREATE SEQUENCE catalogo.productos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: productos_id_seq; Type: SEQUENCE OWNED BY; Schema: catalogo; Owner: -
--

ALTER SEQUENCE catalogo.productos_id_seq OWNED BY catalogo.productos.id;


--
-- Name: zonas; Type: TABLE; Schema: catalogo; Owner: -
--

CREATE TABLE catalogo.zonas (
    id bigint NOT NULL,
    nombre character varying(60) NOT NULL,
    orden integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT zonas_orden_check CHECK ((orden >= 1))
);


--
-- Name: zonas_id_seq; Type: SEQUENCE; Schema: catalogo; Owner: -
--

CREATE SEQUENCE catalogo.zonas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: zonas_id_seq; Type: SEQUENCE OWNED BY; Schema: catalogo; Owner: -
--

ALTER SEQUENCE catalogo.zonas_id_seq OWNED BY catalogo.zonas.id;


--
-- Name: clientes; Type: TABLE; Schema: comercial; Owner: -
--

CREATE TABLE comercial.clientes (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    cedula character varying(20) NOT NULL,
    telefono character varying(20),
    nombre character varying(80) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    creado_at timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: COLUMN clientes.telefono; Type: COMMENT; Schema: comercial; Owner: -
--

COMMENT ON COLUMN comercial.clientes.telefono IS 'Teléfono del cliente. Opcional (NULL permitido): PostgreSQL admite múltiples NULL manteniendo el UNIQUE. No se generan placeholders ni teléfonos falsos.';


--
-- Name: clientes_direcciones; Type: TABLE; Schema: comercial; Owner: -
--

CREATE TABLE comercial.clientes_direcciones (
    id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    etiqueta character varying(40) NOT NULL,
    direccion character varying(200) NOT NULL,
    telefono character varying(20),
    observaciones character varying(200),
    activa boolean DEFAULT true NOT NULL,
    creado_at timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_at timestamp with time zone DEFAULT now() NOT NULL,
    lat numeric(10,7),
    lng numeric(10,7),
    referencia character varying(200)
);


--
-- Name: COLUMN clientes_direcciones.lat; Type: COMMENT; Schema: comercial; Owner: -
--

COMMENT ON COLUMN comercial.clientes_direcciones.lat IS 'Latitud confirmada del marcador en el checkout (grados decimales). NULL en direcciones antiguas.';


--
-- Name: COLUMN clientes_direcciones.lng; Type: COMMENT; Schema: comercial; Owner: -
--

COMMENT ON COLUMN comercial.clientes_direcciones.lng IS 'Longitud confirmada del marcador en el checkout (grados decimales). NULL en direcciones antiguas.';


--
-- Name: clientes_direcciones_id_seq; Type: SEQUENCE; Schema: comercial; Owner: -
--

CREATE SEQUENCE comercial.clientes_direcciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: clientes_direcciones_id_seq; Type: SEQUENCE OWNED BY; Schema: comercial; Owner: -
--

ALTER SEQUENCE comercial.clientes_direcciones_id_seq OWNED BY comercial.clientes_direcciones.id;


--
-- Name: clientes_id_seq; Type: SEQUENCE; Schema: comercial; Owner: -
--

CREATE SEQUENCE comercial.clientes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: clientes_id_seq; Type: SEQUENCE OWNED BY; Schema: comercial; Owner: -
--

ALTER SEQUENCE comercial.clientes_id_seq OWNED BY comercial.clientes.id;


--
-- Name: metodos_pago_cliente; Type: TABLE; Schema: comercial; Owner: -
--

CREATE TABLE comercial.metodos_pago_cliente (
    id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    tipo character varying(20) NOT NULL,
    alias character varying(40) NOT NULL,
    ultimos4 character varying(4),
    predeterminado boolean DEFAULT false NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    creado_at timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT metodos_pago_cliente_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['EFECTIVO'::character varying, 'TARJETA'::character varying, 'TRANSFERENCIA'::character varying, 'OTRO'::character varying])::text[])))
);


--
-- Name: metodos_pago_cliente_id_seq; Type: SEQUENCE; Schema: comercial; Owner: -
--

CREATE SEQUENCE comercial.metodos_pago_cliente_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: metodos_pago_cliente_id_seq; Type: SEQUENCE OWNED BY; Schema: comercial; Owner: -
--

ALTER SEQUENCE comercial.metodos_pago_cliente_id_seq OWNED BY comercial.metodos_pago_cliente.id;


--
-- Name: pedidos_clientes; Type: TABLE; Schema: comercial; Owner: -
--

CREATE TABLE comercial.pedidos_clientes (
    id bigint NOT NULL,
    codigo character varying(40) NOT NULL,
    cliente_id bigint NOT NULL,
    metodo_pago character varying(20) NOT NULL,
    metodo_entrega character varying(20) NOT NULL,
    direccion_id bigint,
    estado character varying(20) NOT NULL,
    idempotency_key character varying(100) NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    confirmado_at timestamp with time zone,
    creado_at timestamp with time zone DEFAULT now() NOT NULL,
    actualizado_at timestamp with time zone DEFAULT now() NOT NULL,
    costo_entrega numeric(12,2) DEFAULT 0.00 NOT NULL,
    direccion_texto character varying(200),
    referencia character varying(200),
    observaciones character varying(500),
    lat numeric(10,7),
    lng numeric(10,7),
    CONSTRAINT pedidos_clientes_estado_check CHECK (((estado)::text = ANY ((ARRAY['BORRADOR'::character varying, 'CONFIRMADO'::character varying, 'EN_PREPARACION'::character varying, 'LISTO'::character varying, 'ENTREGADO'::character varying, 'ANULADO'::character varying])::text[]))),
    CONSTRAINT pedidos_clientes_metodo_entrega_check CHECK (((metodo_entrega)::text = ANY ((ARRAY['RETIRAR'::character varying, 'DOMICILIO'::character varying])::text[]))),
    CONSTRAINT pedidos_clientes_metodo_pago_check CHECK (((metodo_pago)::text = ANY ((ARRAY['EFECTIVO'::character varying, 'TARJETA'::character varying, 'TRANSFERENCIA'::character varying])::text[])))
);


--
-- Name: COLUMN pedidos_clientes.costo_entrega; Type: COMMENT; Schema: comercial; Owner: -
--

COMMENT ON COLUMN comercial.pedidos_clientes.costo_entrega IS 'Recargo de entrega congelado al confirmar: 0.00 para RETIRAR, el recargo configurado (1.50) para DOMICILIO. El backend lo calcula y persiste; no se duplica en reintentos idempotentes.';


--
-- Name: pedidos_clientes_id_seq; Type: SEQUENCE; Schema: comercial; Owner: -
--

CREATE SEQUENCE comercial.pedidos_clientes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pedidos_clientes_id_seq; Type: SEQUENCE OWNED BY; Schema: comercial; Owner: -
--

ALTER SEQUENCE comercial.pedidos_clientes_id_seq OWNED BY comercial.pedidos_clientes.id;


--
-- Name: pedidos_clientes_lineas; Type: TABLE; Schema: comercial; Owner: -
--

CREATE TABLE comercial.pedidos_clientes_lineas (
    id bigint NOT NULL,
    pedido_id bigint NOT NULL,
    producto_id bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    precio numeric(12,2) NOT NULL,
    cantidad integer NOT NULL,
    observaciones character varying(200),
    creado_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pedidos_clientes_lineas_cantidad_check CHECK ((cantidad > 0))
);


--
-- Name: pedidos_clientes_lineas_extras; Type: TABLE; Schema: comercial; Owner: -
--

CREATE TABLE comercial.pedidos_clientes_lineas_extras (
    id bigint NOT NULL,
    linea_id bigint NOT NULL,
    extra_id bigint NOT NULL,
    nombre character varying(100) NOT NULL,
    precio numeric(12,2) NOT NULL,
    creado_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: pedidos_clientes_lineas_extras_id_seq; Type: SEQUENCE; Schema: comercial; Owner: -
--

CREATE SEQUENCE comercial.pedidos_clientes_lineas_extras_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pedidos_clientes_lineas_extras_id_seq; Type: SEQUENCE OWNED BY; Schema: comercial; Owner: -
--

ALTER SEQUENCE comercial.pedidos_clientes_lineas_extras_id_seq OWNED BY comercial.pedidos_clientes_lineas_extras.id;


--
-- Name: pedidos_clientes_lineas_id_seq; Type: SEQUENCE; Schema: comercial; Owner: -
--

CREATE SEQUENCE comercial.pedidos_clientes_lineas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pedidos_clientes_lineas_id_seq; Type: SEQUENCE OWNED BY; Schema: comercial; Owner: -
--

ALTER SEQUENCE comercial.pedidos_clientes_lineas_id_seq OWNED BY comercial.pedidos_clientes_lineas.id;


--
-- Name: impresoras; Type: TABLE; Schema: configuracion; Owner: -
--

CREATE TABLE configuracion.impresoras (
    id bigint NOT NULL,
    nombre character varying(80) NOT NULL,
    tipo character varying(30) NOT NULL,
    ip character varying(45),
    puerto integer DEFAULT 9100 NOT NULL,
    area character varying(100),
    activo boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT impresoras_puerto_check CHECK ((puerto > 0)),
    CONSTRAINT impresoras_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['TERMICA_RED'::character varying, 'TERMICA_USB'::character varying, 'ESCPOS'::character varying])::text[])))
);


--
-- Name: impresoras_id_seq; Type: SEQUENCE; Schema: configuracion; Owner: -
--

CREATE SEQUENCE configuracion.impresoras_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: impresoras_id_seq; Type: SEQUENCE OWNED BY; Schema: configuracion; Owner: -
--

ALTER SEQUENCE configuracion.impresoras_id_seq OWNED BY configuracion.impresoras.id;


--
-- Name: parametros; Type: TABLE; Schema: configuracion; Owner: -
--

CREATE TABLE configuracion.parametros (
    id bigint NOT NULL,
    clave character varying(60) NOT NULL,
    valor character varying(500) NOT NULL,
    tipo character varying(20) DEFAULT 'TEXTO'::character varying NOT NULL,
    descripcion character varying(200),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT parametros_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['TEXTO'::character varying, 'NUMERICO'::character varying, 'BOOLEANO'::character varying])::text[])))
);


--
-- Name: parametros_id_seq; Type: SEQUENCE; Schema: configuracion; Owner: -
--

CREATE SEQUENCE configuracion.parametros_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: parametros_id_seq; Type: SEQUENCE OWNED BY; Schema: configuracion; Owner: -
--

ALTER SEQUENCE configuracion.parametros_id_seq OWNED BY configuracion.parametros.id;


--
-- Name: asignaciones; Type: TABLE; Schema: delivery; Owner: -
--

CREATE TABLE delivery.asignaciones (
    id bigint NOT NULL,
    pedido_cliente_id bigint NOT NULL,
    repartidor_id bigint NOT NULL,
    recorrido_id bigint,
    estado character varying(20) DEFAULT 'ASIGNADA'::character varying NOT NULL,
    asignado_por character varying(50),
    asignado_at timestamp with time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT asignaciones_estado_check CHECK (((estado)::text = ANY ((ARRAY['ASIGNADA'::character varying, 'EN_RUTA'::character varying, 'ENTREGADA'::character varying, 'FALLIDA'::character varying, 'REASIGNADA'::character varying])::text[])))
);


--
-- Name: asignaciones_id_seq; Type: SEQUENCE; Schema: delivery; Owner: -
--

CREATE SEQUENCE delivery.asignaciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: asignaciones_id_seq; Type: SEQUENCE OWNED BY; Schema: delivery; Owner: -
--

ALTER SEQUENCE delivery.asignaciones_id_seq OWNED BY delivery.asignaciones.id;


--
-- Name: config_restaurante; Type: TABLE; Schema: delivery; Owner: -
--

CREATE TABLE delivery.config_restaurante (
    id bigint NOT NULL,
    direccion character varying(200) NOT NULL,
    lat numeric(10,7),
    lng numeric(10,7),
    geocodificado_at timestamp with time zone,
    origen_coords character varying(20),
    fila_unica boolean DEFAULT true NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT config_restaurante_origen_coords_check CHECK (((origen_coords)::text = ANY ((ARRAY['GEOCODING'::character varying, 'MANUAL'::character varying])::text[])))
);


--
-- Name: config_restaurante_id_seq; Type: SEQUENCE; Schema: delivery; Owner: -
--

CREATE SEQUENCE delivery.config_restaurante_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: config_restaurante_id_seq; Type: SEQUENCE OWNED BY; Schema: delivery; Owner: -
--

ALTER SEQUENCE delivery.config_restaurante_id_seq OWNED BY delivery.config_restaurante.id;


--
-- Name: geocoding_cache; Type: TABLE; Schema: delivery; Owner: -
--

CREATE TABLE delivery.geocoding_cache (
    id bigint NOT NULL,
    direccion_id bigint NOT NULL,
    lat numeric(10,7) NOT NULL,
    lng numeric(10,7) NOT NULL,
    direccion_texto character varying(200),
    geocodificado_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: geocoding_cache_id_seq; Type: SEQUENCE; Schema: delivery; Owner: -
--

CREATE SEQUENCE delivery.geocoding_cache_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: geocoding_cache_id_seq; Type: SEQUENCE OWNED BY; Schema: delivery; Owner: -
--

ALTER SEQUENCE delivery.geocoding_cache_id_seq OWNED BY delivery.geocoding_cache.id;


--
-- Name: incidencias; Type: TABLE; Schema: delivery; Owner: -
--

CREATE TABLE delivery.incidencias (
    id bigint NOT NULL,
    parada_id bigint NOT NULL,
    motivo character varying(300) NOT NULL,
    registrado_por character varying(50),
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: incidencias_id_seq; Type: SEQUENCE; Schema: delivery; Owner: -
--

CREATE SEQUENCE delivery.incidencias_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: incidencias_id_seq; Type: SEQUENCE OWNED BY; Schema: delivery; Owner: -
--

ALTER SEQUENCE delivery.incidencias_id_seq OWNED BY delivery.incidencias.id;


--
-- Name: paradas; Type: TABLE; Schema: delivery; Owner: -
--

CREATE TABLE delivery.paradas (
    id bigint NOT NULL,
    recorrido_id bigint NOT NULL,
    asignacion_id bigint NOT NULL,
    orden integer NOT NULL,
    estado character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    entregado_at timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    repartidor_confirmo_at timestamp with time zone,
    repartidor_confirmo_por character varying(50),
    cliente_confirmo_at timestamp with time zone,
    entrega_verificada boolean DEFAULT false NOT NULL,
    resuelta_por_admin character varying(50),
    resuelta_motivo character varying(300),
    CONSTRAINT paradas_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'ENTREGADA'::character varying, 'FALLIDA'::character varying])::text[])))
);


--
-- Name: COLUMN paradas.repartidor_confirmo_at; Type: COMMENT; Schema: delivery; Owner: -
--

COMMENT ON COLUMN delivery.paradas.repartidor_confirmo_at IS 'Fecha/hora en que el repartidor confirmó la llegada/entrega (concepto separado del estado logístico).';


--
-- Name: COLUMN paradas.repartidor_confirmo_por; Type: COMMENT; Schema: delivery; Owner: -
--

COMMENT ON COLUMN delivery.paradas.repartidor_confirmo_por IS 'Usuario repartidor que confirmó la entrega (auditoría de la confirmación 1 de 2).';


--
-- Name: COLUMN paradas.cliente_confirmo_at; Type: COMMENT; Schema: delivery; Owner: -
--

COMMENT ON COLUMN delivery.paradas.cliente_confirmo_at IS 'Fecha/hora en que el cliente confirmó la recepción (confirmación 2 de 2); null mientras esté pendiente.';


--
-- Name: COLUMN paradas.entrega_verificada; Type: COMMENT; Schema: delivery; Owner: -
--

COMMENT ON COLUMN delivery.paradas.entrega_verificada IS 'Estado FINAL verificado: true solo cuando existen ambas confirmaciones (o resolución auditada del admin). Flag independiente del estado logístico.';


--
-- Name: COLUMN paradas.resuelta_por_admin; Type: COMMENT; Schema: delivery; Owner: -
--

COMMENT ON COLUMN delivery.paradas.resuelta_por_admin IS 'Administrador que resolvió la confirmación pendiente del cliente (incidencia); null si no hubo resolución.';


--
-- Name: COLUMN paradas.resuelta_motivo; Type: COMMENT; Schema: delivery; Owner: -
--

COMMENT ON COLUMN delivery.paradas.resuelta_motivo IS 'Motivo justificado de la resolución administrativa de una confirmación pendiente del cliente.';


--
-- Name: paradas_id_seq; Type: SEQUENCE; Schema: delivery; Owner: -
--

CREATE SEQUENCE delivery.paradas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: paradas_id_seq; Type: SEQUENCE OWNED BY; Schema: delivery; Owner: -
--

ALTER SEQUENCE delivery.paradas_id_seq OWNED BY delivery.paradas.id;


--
-- Name: recorridos; Type: TABLE; Schema: delivery; Owner: -
--

CREATE TABLE delivery.recorridos (
    id bigint NOT NULL,
    repartidor_id bigint NOT NULL,
    tipo character varying(20) NOT NULL,
    estado character varying(20) DEFAULT 'PLANIFICADO'::character varying NOT NULL,
    criterio_optimizacion character varying(20) DEFAULT 'DURACION'::character varying NOT NULL,
    distancia_total_m integer,
    duracion_total_s integer,
    iniciado_at timestamp with time zone,
    finalizado_at timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT recorridos_criterio_optimizacion_check CHECK (((criterio_optimizacion)::text = ANY ((ARRAY['DISTANCIA'::character varying, 'DURACION'::character varying])::text[]))),
    CONSTRAINT recorridos_estado_check CHECK (((estado)::text = ANY ((ARRAY['PLANIFICADO'::character varying, 'EN_CURSO'::character varying, 'FINALIZADO'::character varying, 'CANCELADO'::character varying])::text[]))),
    CONSTRAINT recorridos_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['INDIVIDUAL'::character varying, 'MULTIPLE'::character varying])::text[])))
);


--
-- Name: recorridos_id_seq; Type: SEQUENCE; Schema: delivery; Owner: -
--

CREATE SEQUENCE delivery.recorridos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: recorridos_id_seq; Type: SEQUENCE OWNED BY; Schema: delivery; Owner: -
--

ALTER SEQUENCE delivery.recorridos_id_seq OWNED BY delivery.recorridos.id;


--
-- Name: repartidores; Type: TABLE; Schema: delivery; Owner: -
--

CREATE TABLE delivery.repartidores (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    estado character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    habilitado_por character varying(50),
    habilitado_at timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT repartidores_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'HABILITADO'::character varying, 'SUSPENDIDO'::character varying])::text[])))
);


--
-- Name: repartidores_id_seq; Type: SEQUENCE; Schema: delivery; Owner: -
--

CREATE SEQUENCE delivery.repartidores_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: repartidores_id_seq; Type: SEQUENCE OWNED BY; Schema: delivery; Owner: -
--

ALTER SEQUENCE delivery.repartidores_id_seq OWNED BY delivery.repartidores.id;


--
-- Name: rutas_cache; Type: TABLE; Schema: delivery; Owner: -
--

CREATE TABLE delivery.rutas_cache (
    id bigint NOT NULL,
    recorrido_id bigint,
    clave_hash character varying(64) NOT NULL,
    geometria text,
    distancia_m integer,
    duracion_s integer,
    calculado_at timestamp with time zone DEFAULT now() NOT NULL,
    expira_at timestamp with time zone
);


--
-- Name: rutas_cache_id_seq; Type: SEQUENCE; Schema: delivery; Owner: -
--

CREATE SEQUENCE delivery.rutas_cache_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: rutas_cache_id_seq; Type: SEQUENCE OWNED BY; Schema: delivery; Owner: -
--

ALTER SEQUENCE delivery.rutas_cache_id_seq OWNED BY delivery.rutas_cache.id;


--
-- Name: anulaciones; Type: TABLE; Schema: operaciones; Owner: -
--

CREATE TABLE operaciones.anulaciones (
    id bigint NOT NULL,
    pedido_codigo character varying(40) NOT NULL,
    linea_id bigint NOT NULL,
    producto_id bigint NOT NULL,
    nombre_producto character varying(120) NOT NULL,
    precio_unitario numeric(12,2) NOT NULL,
    cantidad integer NOT NULL,
    motivo character varying(300),
    area_id bigint,
    area_nombre character varying(80),
    estado character varying(20) DEFAULT 'SOLICITADA'::character varying NOT NULL,
    solicitado_por character varying(50),
    resuelto_por character varying(50),
    resuelta_at timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT anulaciones_cantidad_check CHECK ((cantidad > 0)),
    CONSTRAINT anulaciones_estado_check CHECK (((estado)::text = ANY ((ARRAY['SOLICITADA'::character varying, 'APROBADA'::character varying, 'RECHAZADA'::character varying])::text[]))),
    CONSTRAINT anulaciones_precio_unitario_check CHECK ((precio_unitario >= (0)::numeric))
);


--
-- Name: anulaciones_id_seq; Type: SEQUENCE; Schema: operaciones; Owner: -
--

CREATE SEQUENCE operaciones.anulaciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: anulaciones_id_seq; Type: SEQUENCE OWNED BY; Schema: operaciones; Owner: -
--

ALTER SEQUENCE operaciones.anulaciones_id_seq OWNED BY operaciones.anulaciones.id;


--
-- Name: comanda_lineas; Type: TABLE; Schema: operaciones; Owner: -
--

CREATE TABLE operaciones.comanda_lineas (
    id bigint NOT NULL,
    comanda_id bigint NOT NULL,
    producto_id bigint NOT NULL,
    nombre_producto character varying(120) NOT NULL,
    cantidad integer NOT NULL,
    extras character varying(300),
    ingredientes character varying(300),
    observaciones character varying(500),
    orden integer DEFAULT 0 NOT NULL,
    CONSTRAINT comanda_lineas_cantidad_check CHECK ((cantidad > 0))
);


--
-- Name: comanda_lineas_id_seq; Type: SEQUENCE; Schema: operaciones; Owner: -
--

CREATE SEQUENCE operaciones.comanda_lineas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: comanda_lineas_id_seq; Type: SEQUENCE OWNED BY; Schema: operaciones; Owner: -
--

ALTER SEQUENCE operaciones.comanda_lineas_id_seq OWNED BY operaciones.comanda_lineas.id;


--
-- Name: comandas; Type: TABLE; Schema: operaciones; Owner: -
--

CREATE TABLE operaciones.comandas (
    id bigint NOT NULL,
    pedido_codigo character varying(40) NOT NULL,
    numero_comanda integer NOT NULL,
    area_id bigint NOT NULL,
    area_nombre character varying(80) NOT NULL,
    estado character varying(20) DEFAULT 'PENDIENTE'::character varying NOT NULL,
    tipo character varying(20) DEFAULT 'ORDEN'::character varying NOT NULL,
    anulacion_id bigint,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT comandas_estado_check CHECK (((estado)::text = ANY ((ARRAY['PENDIENTE'::character varying, 'EN_PREPARACION'::character varying, 'LISTO'::character varying])::text[]))),
    CONSTRAINT comandas_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['ORDEN'::character varying, 'CANCELACION'::character varying])::text[])))
);


--
-- Name: comandas_id_seq; Type: SEQUENCE; Schema: operaciones; Owner: -
--

CREATE SEQUENCE operaciones.comandas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: comandas_id_seq; Type: SEQUENCE OWNED BY; Schema: operaciones; Owner: -
--

ALTER SEQUENCE operaciones.comandas_id_seq OWNED BY operaciones.comandas.id;


--
-- Name: confirmaciones; Type: TABLE; Schema: operaciones; Owner: -
--

CREATE TABLE operaciones.confirmaciones (
    id bigint NOT NULL,
    pedido_id bigint NOT NULL,
    idempotency_key character varying(100) NOT NULL,
    confirmado_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: confirmaciones_id_seq; Type: SEQUENCE; Schema: operaciones; Owner: -
--

CREATE SEQUENCE operaciones.confirmaciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: confirmaciones_id_seq; Type: SEQUENCE OWNED BY; Schema: operaciones; Owner: -
--

ALTER SEQUENCE operaciones.confirmaciones_id_seq OWNED BY operaciones.confirmaciones.id;


--
-- Name: cuentas; Type: TABLE; Schema: operaciones; Owner: -
--

CREATE TABLE operaciones.cuentas (
    id bigint NOT NULL,
    mesa_id bigint NOT NULL,
    estado character varying(20) DEFAULT 'ABIERTA'::character varying NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT cuentas_estado_check CHECK (((estado)::text = ANY ((ARRAY['ABIERTA'::character varying, 'CERRADA'::character varying])::text[])))
);


--
-- Name: cuentas_id_seq; Type: SEQUENCE; Schema: operaciones; Owner: -
--

CREATE SEQUENCE operaciones.cuentas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cuentas_id_seq; Type: SEQUENCE OWNED BY; Schema: operaciones; Owner: -
--

ALTER SEQUENCE operaciones.cuentas_id_seq OWNED BY operaciones.cuentas.id;


--
-- Name: pedido_linea_extras; Type: TABLE; Schema: operaciones; Owner: -
--

CREATE TABLE operaciones.pedido_linea_extras (
    id bigint NOT NULL,
    linea_id bigint NOT NULL,
    extra_id bigint NOT NULL,
    nombre_extra character varying(80) NOT NULL,
    precio numeric(12,2) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT pedido_linea_extras_precio_check CHECK ((precio >= (0)::numeric))
);


--
-- Name: pedido_linea_extras_id_seq; Type: SEQUENCE; Schema: operaciones; Owner: -
--

CREATE SEQUENCE operaciones.pedido_linea_extras_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pedido_linea_extras_id_seq; Type: SEQUENCE OWNED BY; Schema: operaciones; Owner: -
--

ALTER SEQUENCE operaciones.pedido_linea_extras_id_seq OWNED BY operaciones.pedido_linea_extras.id;


--
-- Name: pedido_linea_ingredientes; Type: TABLE; Schema: operaciones; Owner: -
--

CREATE TABLE operaciones.pedido_linea_ingredientes (
    id bigint NOT NULL,
    linea_id bigint NOT NULL,
    nombre character varying(80) NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: pedido_linea_ingredientes_id_seq; Type: SEQUENCE; Schema: operaciones; Owner: -
--

CREATE SEQUENCE operaciones.pedido_linea_ingredientes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pedido_linea_ingredientes_id_seq; Type: SEQUENCE OWNED BY; Schema: operaciones; Owner: -
--

ALTER SEQUENCE operaciones.pedido_linea_ingredientes_id_seq OWNED BY operaciones.pedido_linea_ingredientes.id;


--
-- Name: pedido_lineas; Type: TABLE; Schema: operaciones; Owner: -
--

CREATE TABLE operaciones.pedido_lineas (
    id bigint NOT NULL,
    pedido_id bigint NOT NULL,
    producto_id bigint NOT NULL,
    nombre_producto character varying(120) NOT NULL,
    precio_unitario numeric(12,2) NOT NULL,
    cantidad integer NOT NULL,
    observaciones character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT pedido_lineas_cantidad_check CHECK ((cantidad > 0)),
    CONSTRAINT pedido_lineas_precio_unitario_check CHECK ((precio_unitario >= (0)::numeric))
);


--
-- Name: pedido_lineas_id_seq; Type: SEQUENCE; Schema: operaciones; Owner: -
--

CREATE SEQUENCE operaciones.pedido_lineas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pedido_lineas_id_seq; Type: SEQUENCE OWNED BY; Schema: operaciones; Owner: -
--

ALTER SEQUENCE operaciones.pedido_lineas_id_seq OWNED BY operaciones.pedido_lineas.id;


--
-- Name: pedidos; Type: TABLE; Schema: operaciones; Owner: -
--

CREATE TABLE operaciones.pedidos (
    id bigint NOT NULL,
    codigo character varying(40) NOT NULL,
    mesa_id bigint,
    cuenta_id bigint,
    estado character varying(20) DEFAULT 'BORRADOR'::character varying NOT NULL,
    notas character varying(500),
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT pedidos_estado_check CHECK (((estado)::text = ANY ((ARRAY['BORRADOR'::character varying, 'CONFIRMADO'::character varying, 'EN_PREPARACION'::character varying, 'LISTO'::character varying, 'ENTREGADO'::character varying, 'ANULADO'::character varying])::text[])))
);


--
-- Name: pedidos_id_seq; Type: SEQUENCE; Schema: operaciones; Owner: -
--

CREATE SEQUENCE operaciones.pedidos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pedidos_id_seq; Type: SEQUENCE OWNED BY; Schema: operaciones; Owner: -
--

ALTER SEQUENCE operaciones.pedidos_id_seq OWNED BY operaciones.pedidos.id;


--
-- Name: seq_numero_comanda; Type: SEQUENCE; Schema: operaciones; Owner: -
--

CREATE SEQUENCE operaciones.seq_numero_comanda
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: categorias_roles; Type: TABLE; Schema: seguridad; Owner: -
--

CREATE TABLE seguridad.categorias_roles (
    id bigint NOT NULL,
    codigo character varying(30) NOT NULL,
    descripcion character varying(120)
);


--
-- Name: categorias_roles_id_seq; Type: SEQUENCE; Schema: seguridad; Owner: -
--

CREATE SEQUENCE seguridad.categorias_roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: categorias_roles_id_seq; Type: SEQUENCE OWNED BY; Schema: seguridad; Owner: -
--

ALTER SEQUENCE seguridad.categorias_roles_id_seq OWNED BY seguridad.categorias_roles.id;


--
-- Name: permisos; Type: TABLE; Schema: seguridad; Owner: -
--

CREATE TABLE seguridad.permisos (
    id bigint NOT NULL,
    codigo character varying(60) NOT NULL,
    modulo character varying(30) NOT NULL,
    descripcion character varying(120),
    activo boolean DEFAULT true NOT NULL
);


--
-- Name: permisos_id_seq; Type: SEQUENCE; Schema: seguridad; Owner: -
--

CREATE SEQUENCE seguridad.permisos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: permisos_id_seq; Type: SEQUENCE OWNED BY; Schema: seguridad; Owner: -
--

ALTER SEQUENCE seguridad.permisos_id_seq OWNED BY seguridad.permisos.id;


--
-- Name: recuperacion_password; Type: TABLE; Schema: seguridad; Owner: -
--

CREATE TABLE seguridad.recuperacion_password (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    token_hash character varying(64) NOT NULL,
    expira_at timestamp with time zone NOT NULL,
    usado boolean DEFAULT false NOT NULL,
    creado_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: recuperacion_password_id_seq; Type: SEQUENCE; Schema: seguridad; Owner: -
--

CREATE SEQUENCE seguridad.recuperacion_password_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: recuperacion_password_id_seq; Type: SEQUENCE OWNED BY; Schema: seguridad; Owner: -
--

ALTER SEQUENCE seguridad.recuperacion_password_id_seq OWNED BY seguridad.recuperacion_password.id;


--
-- Name: refresh_tokens; Type: TABLE; Schema: seguridad; Owner: -
--

CREATE TABLE seguridad.refresh_tokens (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    token_hash character varying(64) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    revoked boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE; Schema: seguridad; Owner: -
--

CREATE SEQUENCE seguridad.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: seguridad; Owner: -
--

ALTER SEQUENCE seguridad.refresh_tokens_id_seq OWNED BY seguridad.refresh_tokens.id;


--
-- Name: roles; Type: TABLE; Schema: seguridad; Owner: -
--

CREATE TABLE seguridad.roles (
    id bigint NOT NULL,
    codigo character varying(30) NOT NULL,
    descripcion character varying(120),
    activo boolean DEFAULT true NOT NULL,
    categoria_id bigint NOT NULL,
    asignable_por_admin boolean DEFAULT true NOT NULL,
    configurable boolean DEFAULT false NOT NULL,
    origen character varying(20) DEFAULT 'ADMIN'::character varying NOT NULL
);


--
-- Name: roles_id_seq; Type: SEQUENCE; Schema: seguridad; Owner: -
--

CREATE SEQUENCE seguridad.roles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: roles_id_seq; Type: SEQUENCE OWNED BY; Schema: seguridad; Owner: -
--

ALTER SEQUENCE seguridad.roles_id_seq OWNED BY seguridad.roles.id;


--
-- Name: roles_permisos; Type: TABLE; Schema: seguridad; Owner: -
--

CREATE TABLE seguridad.roles_permisos (
    rol_id bigint NOT NULL,
    permiso_id bigint NOT NULL
);


--
-- Name: usuarios; Type: TABLE; Schema: seguridad; Owner: -
--

CREATE TABLE seguridad.usuarios (
    id bigint NOT NULL,
    username character varying(50) NOT NULL,
    password_hash character varying(100) NOT NULL,
    nombre character varying(100) NOT NULL,
    activo boolean DEFAULT true NOT NULL,
    rol_id bigint NOT NULL,
    sesion_version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    email_verificado boolean DEFAULT true NOT NULL,
    celular character varying(20)
);


--
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: seguridad; Owner: -
--

CREATE SEQUENCE seguridad.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: usuarios_id_seq; Type: SEQUENCE OWNED BY; Schema: seguridad; Owner: -
--

ALTER SEQUENCE seguridad.usuarios_id_seq OWNED BY seguridad.usuarios.id;


--
-- Name: usuarios_permisos; Type: TABLE; Schema: seguridad; Owner: -
--

CREATE TABLE seguridad.usuarios_permisos (
    usuario_id bigint NOT NULL,
    permiso_id bigint NOT NULL
);


--
-- Name: verificacion_email; Type: TABLE; Schema: seguridad; Owner: -
--

CREATE TABLE seguridad.verificacion_email (
    id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    codigo_hash character varying(64) NOT NULL,
    expira_at timestamp with time zone NOT NULL,
    usado boolean DEFAULT false NOT NULL,
    creado_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: verificacion_email_id_seq; Type: SEQUENCE; Schema: seguridad; Owner: -
--

CREATE SEQUENCE seguridad.verificacion_email_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: verificacion_email_id_seq; Type: SEQUENCE OWNED BY; Schema: seguridad; Owner: -
--

ALTER SEQUENCE seguridad.verificacion_email_id_seq OWNED BY seguridad.verificacion_email.id;


--
-- Name: caja_movimientos; Type: TABLE; Schema: tesoreria; Owner: -
--

CREATE TABLE tesoreria.caja_movimientos (
    id bigint NOT NULL,
    caja_id bigint NOT NULL,
    tipo character varying(10) NOT NULL,
    concepto character varying(120) NOT NULL,
    monto numeric(12,2) NOT NULL,
    metodo character varying(20),
    pago_id bigint,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT caja_movimientos_metodo_check CHECK (((metodo)::text = ANY ((ARRAY['EFECTIVO'::character varying, 'TARJETA'::character varying, 'TRANSFERENCIA'::character varying, 'OTRO'::character varying])::text[]))),
    CONSTRAINT caja_movimientos_monto_check CHECK ((monto > (0)::numeric)),
    CONSTRAINT caja_movimientos_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['INGRESO'::character varying, 'EGRESO'::character varying])::text[])))
);


--
-- Name: caja_movimientos_id_seq; Type: SEQUENCE; Schema: tesoreria; Owner: -
--

CREATE SEQUENCE tesoreria.caja_movimientos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: caja_movimientos_id_seq; Type: SEQUENCE OWNED BY; Schema: tesoreria; Owner: -
--

ALTER SEQUENCE tesoreria.caja_movimientos_id_seq OWNED BY tesoreria.caja_movimientos.id;


--
-- Name: cajas; Type: TABLE; Schema: tesoreria; Owner: -
--

CREATE TABLE tesoreria.cajas (
    id bigint NOT NULL,
    estado character varying(20) DEFAULT 'ABIERTA'::character varying NOT NULL,
    apertura_inicial numeric(12,2) DEFAULT 0 NOT NULL,
    cierre_esperado numeric(12,2),
    cierre_real numeric(12,2),
    diferencia numeric(12,2),
    abierta_por character varying(50),
    cerrada_por character varying(50),
    abierta_at timestamp with time zone DEFAULT now() NOT NULL,
    cerrada_at timestamp with time zone,
    version bigint DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT cajas_apertura_inicial_check CHECK ((apertura_inicial >= (0)::numeric)),
    CONSTRAINT cajas_estado_check CHECK (((estado)::text = ANY ((ARRAY['ABIERTA'::character varying, 'CERRADA'::character varying])::text[])))
);


--
-- Name: cajas_id_seq; Type: SEQUENCE; Schema: tesoreria; Owner: -
--

CREATE SEQUENCE tesoreria.cajas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cajas_id_seq; Type: SEQUENCE OWNED BY; Schema: tesoreria; Owner: -
--

ALTER SEQUENCE tesoreria.cajas_id_seq OWNED BY tesoreria.cajas.id;


--
-- Name: comprobantes; Type: TABLE; Schema: tesoreria; Owner: -
--

CREATE TABLE tesoreria.comprobantes (
    id bigint NOT NULL,
    correlativo character varying(20) NOT NULL,
    secuencial bigint NOT NULL,
    tipo character varying(10) NOT NULL,
    cuenta_id bigint NOT NULL,
    fecha timestamp with time zone DEFAULT now() NOT NULL,
    cliente_nombre character varying(120),
    cliente_identificacion character varying(20),
    total numeric(12,2) NOT NULL,
    emitido_por character varying(50),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT comprobantes_tipo_check CHECK (((tipo)::text = ANY ((ARRAY['FACTURA'::character varying, 'TICKET'::character varying])::text[])))
);


--
-- Name: comprobantes_id_seq; Type: SEQUENCE; Schema: tesoreria; Owner: -
--

CREATE SEQUENCE tesoreria.comprobantes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: comprobantes_id_seq; Type: SEQUENCE OWNED BY; Schema: tesoreria; Owner: -
--

ALTER SEQUENCE tesoreria.comprobantes_id_seq OWNED BY tesoreria.comprobantes.id;


--
-- Name: pagos; Type: TABLE; Schema: tesoreria; Owner: -
--

CREATE TABLE tesoreria.pagos (
    id bigint NOT NULL,
    cuenta_id bigint NOT NULL,
    caja_id bigint NOT NULL,
    metodo character varying(20) NOT NULL,
    monto numeric(12,2) NOT NULL,
    cobrado_por character varying(50),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_by character varying(50),
    CONSTRAINT pagos_metodo_check CHECK (((metodo)::text = ANY ((ARRAY['EFECTIVO'::character varying, 'TARJETA'::character varying, 'TRANSFERENCIA'::character varying, 'OTRO'::character varying])::text[]))),
    CONSTRAINT pagos_monto_check CHECK ((monto > (0)::numeric))
);


--
-- Name: pagos_id_seq; Type: SEQUENCE; Schema: tesoreria; Owner: -
--

CREATE SEQUENCE tesoreria.pagos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pagos_id_seq; Type: SEQUENCE OWNED BY; Schema: tesoreria; Owner: -
--

ALTER SEQUENCE tesoreria.pagos_id_seq OWNED BY tesoreria.pagos.id;


--
-- Name: seq_correlativo_comprobante; Type: SEQUENCE; Schema: tesoreria; Owner: -
--

CREATE SEQUENCE tesoreria.seq_correlativo_comprobante
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: auditoria_eventos id; Type: DEFAULT; Schema: auditoria; Owner: -
--

ALTER TABLE ONLY auditoria.auditoria_eventos ALTER COLUMN id SET DEFAULT nextval('auditoria.auditoria_eventos_id_seq'::regclass);


--
-- Name: outbox id; Type: DEFAULT; Schema: auditoria; Owner: -
--

ALTER TABLE ONLY auditoria.outbox ALTER COLUMN id SET DEFAULT nextval('auditoria.outbox_id_seq'::regclass);


--
-- Name: areas id; Type: DEFAULT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.areas ALTER COLUMN id SET DEFAULT nextval('catalogo.areas_id_seq'::regclass);


--
-- Name: categorias id; Type: DEFAULT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.categorias ALTER COLUMN id SET DEFAULT nextval('catalogo.categorias_id_seq'::regclass);


--
-- Name: extras id; Type: DEFAULT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.extras ALTER COLUMN id SET DEFAULT nextval('catalogo.extras_id_seq'::regclass);


--
-- Name: mesas id; Type: DEFAULT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.mesas ALTER COLUMN id SET DEFAULT nextval('catalogo.mesas_id_seq'::regclass);


--
-- Name: producto_ingredientes id; Type: DEFAULT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.producto_ingredientes ALTER COLUMN id SET DEFAULT nextval('catalogo.producto_ingredientes_id_seq'::regclass);


--
-- Name: productos id; Type: DEFAULT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.productos ALTER COLUMN id SET DEFAULT nextval('catalogo.productos_id_seq'::regclass);


--
-- Name: zonas id; Type: DEFAULT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.zonas ALTER COLUMN id SET DEFAULT nextval('catalogo.zonas_id_seq'::regclass);


--
-- Name: clientes id; Type: DEFAULT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.clientes ALTER COLUMN id SET DEFAULT nextval('comercial.clientes_id_seq'::regclass);


--
-- Name: clientes_direcciones id; Type: DEFAULT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.clientes_direcciones ALTER COLUMN id SET DEFAULT nextval('comercial.clientes_direcciones_id_seq'::regclass);


--
-- Name: metodos_pago_cliente id; Type: DEFAULT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.metodos_pago_cliente ALTER COLUMN id SET DEFAULT nextval('comercial.metodos_pago_cliente_id_seq'::regclass);


--
-- Name: pedidos_clientes id; Type: DEFAULT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes ALTER COLUMN id SET DEFAULT nextval('comercial.pedidos_clientes_id_seq'::regclass);


--
-- Name: pedidos_clientes_lineas id; Type: DEFAULT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes_lineas ALTER COLUMN id SET DEFAULT nextval('comercial.pedidos_clientes_lineas_id_seq'::regclass);


--
-- Name: pedidos_clientes_lineas_extras id; Type: DEFAULT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes_lineas_extras ALTER COLUMN id SET DEFAULT nextval('comercial.pedidos_clientes_lineas_extras_id_seq'::regclass);


--
-- Name: impresoras id; Type: DEFAULT; Schema: configuracion; Owner: -
--

ALTER TABLE ONLY configuracion.impresoras ALTER COLUMN id SET DEFAULT nextval('configuracion.impresoras_id_seq'::regclass);


--
-- Name: parametros id; Type: DEFAULT; Schema: configuracion; Owner: -
--

ALTER TABLE ONLY configuracion.parametros ALTER COLUMN id SET DEFAULT nextval('configuracion.parametros_id_seq'::regclass);


--
-- Name: asignaciones id; Type: DEFAULT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.asignaciones ALTER COLUMN id SET DEFAULT nextval('delivery.asignaciones_id_seq'::regclass);


--
-- Name: config_restaurante id; Type: DEFAULT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.config_restaurante ALTER COLUMN id SET DEFAULT nextval('delivery.config_restaurante_id_seq'::regclass);


--
-- Name: geocoding_cache id; Type: DEFAULT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.geocoding_cache ALTER COLUMN id SET DEFAULT nextval('delivery.geocoding_cache_id_seq'::regclass);


--
-- Name: incidencias id; Type: DEFAULT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.incidencias ALTER COLUMN id SET DEFAULT nextval('delivery.incidencias_id_seq'::regclass);


--
-- Name: paradas id; Type: DEFAULT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.paradas ALTER COLUMN id SET DEFAULT nextval('delivery.paradas_id_seq'::regclass);


--
-- Name: recorridos id; Type: DEFAULT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.recorridos ALTER COLUMN id SET DEFAULT nextval('delivery.recorridos_id_seq'::regclass);


--
-- Name: repartidores id; Type: DEFAULT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.repartidores ALTER COLUMN id SET DEFAULT nextval('delivery.repartidores_id_seq'::regclass);


--
-- Name: rutas_cache id; Type: DEFAULT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.rutas_cache ALTER COLUMN id SET DEFAULT nextval('delivery.rutas_cache_id_seq'::regclass);


--
-- Name: anulaciones id; Type: DEFAULT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.anulaciones ALTER COLUMN id SET DEFAULT nextval('operaciones.anulaciones_id_seq'::regclass);


--
-- Name: comanda_lineas id; Type: DEFAULT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.comanda_lineas ALTER COLUMN id SET DEFAULT nextval('operaciones.comanda_lineas_id_seq'::regclass);


--
-- Name: comandas id; Type: DEFAULT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.comandas ALTER COLUMN id SET DEFAULT nextval('operaciones.comandas_id_seq'::regclass);


--
-- Name: confirmaciones id; Type: DEFAULT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.confirmaciones ALTER COLUMN id SET DEFAULT nextval('operaciones.confirmaciones_id_seq'::regclass);


--
-- Name: cuentas id; Type: DEFAULT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.cuentas ALTER COLUMN id SET DEFAULT nextval('operaciones.cuentas_id_seq'::regclass);


--
-- Name: pedido_linea_extras id; Type: DEFAULT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedido_linea_extras ALTER COLUMN id SET DEFAULT nextval('operaciones.pedido_linea_extras_id_seq'::regclass);


--
-- Name: pedido_linea_ingredientes id; Type: DEFAULT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedido_linea_ingredientes ALTER COLUMN id SET DEFAULT nextval('operaciones.pedido_linea_ingredientes_id_seq'::regclass);


--
-- Name: pedido_lineas id; Type: DEFAULT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedido_lineas ALTER COLUMN id SET DEFAULT nextval('operaciones.pedido_lineas_id_seq'::regclass);


--
-- Name: pedidos id; Type: DEFAULT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedidos ALTER COLUMN id SET DEFAULT nextval('operaciones.pedidos_id_seq'::regclass);


--
-- Name: categorias_roles id; Type: DEFAULT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.categorias_roles ALTER COLUMN id SET DEFAULT nextval('seguridad.categorias_roles_id_seq'::regclass);


--
-- Name: permisos id; Type: DEFAULT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.permisos ALTER COLUMN id SET DEFAULT nextval('seguridad.permisos_id_seq'::regclass);


--
-- Name: recuperacion_password id; Type: DEFAULT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.recuperacion_password ALTER COLUMN id SET DEFAULT nextval('seguridad.recuperacion_password_id_seq'::regclass);


--
-- Name: refresh_tokens id; Type: DEFAULT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('seguridad.refresh_tokens_id_seq'::regclass);


--
-- Name: roles id; Type: DEFAULT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.roles ALTER COLUMN id SET DEFAULT nextval('seguridad.roles_id_seq'::regclass);


--
-- Name: usuarios id; Type: DEFAULT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.usuarios ALTER COLUMN id SET DEFAULT nextval('seguridad.usuarios_id_seq'::regclass);


--
-- Name: verificacion_email id; Type: DEFAULT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.verificacion_email ALTER COLUMN id SET DEFAULT nextval('seguridad.verificacion_email_id_seq'::regclass);


--
-- Name: caja_movimientos id; Type: DEFAULT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.caja_movimientos ALTER COLUMN id SET DEFAULT nextval('tesoreria.caja_movimientos_id_seq'::regclass);


--
-- Name: cajas id; Type: DEFAULT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.cajas ALTER COLUMN id SET DEFAULT nextval('tesoreria.cajas_id_seq'::regclass);


--
-- Name: comprobantes id; Type: DEFAULT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.comprobantes ALTER COLUMN id SET DEFAULT nextval('tesoreria.comprobantes_id_seq'::regclass);


--
-- Name: pagos id; Type: DEFAULT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.pagos ALTER COLUMN id SET DEFAULT nextval('tesoreria.pagos_id_seq'::regclass);


--
-- Name: archivos archivos_clave_key; Type: CONSTRAINT; Schema: archivos; Owner: -
--

ALTER TABLE ONLY archivos.archivos
    ADD CONSTRAINT archivos_clave_key UNIQUE (clave);


--
-- Name: archivos archivos_pkey; Type: CONSTRAINT; Schema: archivos; Owner: -
--

ALTER TABLE ONLY archivos.archivos
    ADD CONSTRAINT archivos_pkey PRIMARY KEY (id);


--
-- Name: auditoria_eventos auditoria_eventos_pkey; Type: CONSTRAINT; Schema: auditoria; Owner: -
--

ALTER TABLE ONLY auditoria.auditoria_eventos
    ADD CONSTRAINT auditoria_eventos_pkey PRIMARY KEY (id);


--
-- Name: outbox outbox_pkey; Type: CONSTRAINT; Schema: auditoria; Owner: -
--

ALTER TABLE ONLY auditoria.outbox
    ADD CONSTRAINT outbox_pkey PRIMARY KEY (id);


--
-- Name: areas areas_nombre_key; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.areas
    ADD CONSTRAINT areas_nombre_key UNIQUE (nombre);


--
-- Name: areas areas_pkey; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.areas
    ADD CONSTRAINT areas_pkey PRIMARY KEY (id);


--
-- Name: categorias categorias_nombre_key; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.categorias
    ADD CONSTRAINT categorias_nombre_key UNIQUE (nombre);


--
-- Name: categorias categorias_pkey; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.categorias
    ADD CONSTRAINT categorias_pkey PRIMARY KEY (id);


--
-- Name: extras extras_nombre_key; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.extras
    ADD CONSTRAINT extras_nombre_key UNIQUE (nombre);


--
-- Name: extras extras_pkey; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.extras
    ADD CONSTRAINT extras_pkey PRIMARY KEY (id);


--
-- Name: mesas mesas_numero_key; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.mesas
    ADD CONSTRAINT mesas_numero_key UNIQUE (numero);


--
-- Name: mesas mesas_pkey; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.mesas
    ADD CONSTRAINT mesas_pkey PRIMARY KEY (id);


--
-- Name: producto_extras producto_extras_pkey; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.producto_extras
    ADD CONSTRAINT producto_extras_pkey PRIMARY KEY (producto_id, extra_id);


--
-- Name: producto_ingredientes producto_ingredientes_pkey; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.producto_ingredientes
    ADD CONSTRAINT producto_ingredientes_pkey PRIMARY KEY (id);


--
-- Name: productos productos_nombre_key; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.productos
    ADD CONSTRAINT productos_nombre_key UNIQUE (nombre);


--
-- Name: productos productos_pkey; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.productos
    ADD CONSTRAINT productos_pkey PRIMARY KEY (id);


--
-- Name: zonas zonas_pkey; Type: CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.zonas
    ADD CONSTRAINT zonas_pkey PRIMARY KEY (id);


--
-- Name: clientes clientes_cedula_key; Type: CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.clientes
    ADD CONSTRAINT clientes_cedula_key UNIQUE (cedula);


--
-- Name: clientes_direcciones clientes_direcciones_pkey; Type: CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.clientes_direcciones
    ADD CONSTRAINT clientes_direcciones_pkey PRIMARY KEY (id);


--
-- Name: clientes clientes_pkey; Type: CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.clientes
    ADD CONSTRAINT clientes_pkey PRIMARY KEY (id);


--
-- Name: clientes clientes_telefono_key; Type: CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.clientes
    ADD CONSTRAINT clientes_telefono_key UNIQUE (telefono);


--
-- Name: clientes clientes_usuario_id_key; Type: CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.clientes
    ADD CONSTRAINT clientes_usuario_id_key UNIQUE (usuario_id);


--
-- Name: metodos_pago_cliente metodos_pago_cliente_pkey; Type: CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.metodos_pago_cliente
    ADD CONSTRAINT metodos_pago_cliente_pkey PRIMARY KEY (id);


--
-- Name: pedidos_clientes pedidos_clientes_codigo_key; Type: CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes
    ADD CONSTRAINT pedidos_clientes_codigo_key UNIQUE (codigo);


--
-- Name: pedidos_clientes_lineas_extras pedidos_clientes_lineas_extras_pkey; Type: CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes_lineas_extras
    ADD CONSTRAINT pedidos_clientes_lineas_extras_pkey PRIMARY KEY (id);


--
-- Name: pedidos_clientes_lineas pedidos_clientes_lineas_pkey; Type: CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes_lineas
    ADD CONSTRAINT pedidos_clientes_lineas_pkey PRIMARY KEY (id);


--
-- Name: pedidos_clientes pedidos_clientes_pkey; Type: CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes
    ADD CONSTRAINT pedidos_clientes_pkey PRIMARY KEY (id);


--
-- Name: pedidos_clientes uq_pedido_cliente_idempotencia; Type: CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes
    ADD CONSTRAINT uq_pedido_cliente_idempotencia UNIQUE (cliente_id, idempotency_key);


--
-- Name: impresoras impresoras_nombre_key; Type: CONSTRAINT; Schema: configuracion; Owner: -
--

ALTER TABLE ONLY configuracion.impresoras
    ADD CONSTRAINT impresoras_nombre_key UNIQUE (nombre);


--
-- Name: impresoras impresoras_pkey; Type: CONSTRAINT; Schema: configuracion; Owner: -
--

ALTER TABLE ONLY configuracion.impresoras
    ADD CONSTRAINT impresoras_pkey PRIMARY KEY (id);


--
-- Name: parametros parametros_clave_key; Type: CONSTRAINT; Schema: configuracion; Owner: -
--

ALTER TABLE ONLY configuracion.parametros
    ADD CONSTRAINT parametros_clave_key UNIQUE (clave);


--
-- Name: parametros parametros_pkey; Type: CONSTRAINT; Schema: configuracion; Owner: -
--

ALTER TABLE ONLY configuracion.parametros
    ADD CONSTRAINT parametros_pkey PRIMARY KEY (id);


--
-- Name: asignaciones asignaciones_pkey; Type: CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.asignaciones
    ADD CONSTRAINT asignaciones_pkey PRIMARY KEY (id);


--
-- Name: config_restaurante config_restaurante_pkey; Type: CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.config_restaurante
    ADD CONSTRAINT config_restaurante_pkey PRIMARY KEY (id);


--
-- Name: geocoding_cache geocoding_cache_direccion_id_key; Type: CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.geocoding_cache
    ADD CONSTRAINT geocoding_cache_direccion_id_key UNIQUE (direccion_id);


--
-- Name: geocoding_cache geocoding_cache_pkey; Type: CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.geocoding_cache
    ADD CONSTRAINT geocoding_cache_pkey PRIMARY KEY (id);


--
-- Name: incidencias incidencias_pkey; Type: CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.incidencias
    ADD CONSTRAINT incidencias_pkey PRIMARY KEY (id);


--
-- Name: paradas paradas_pkey; Type: CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.paradas
    ADD CONSTRAINT paradas_pkey PRIMARY KEY (id);


--
-- Name: recorridos recorridos_pkey; Type: CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.recorridos
    ADD CONSTRAINT recorridos_pkey PRIMARY KEY (id);


--
-- Name: repartidores repartidores_pkey; Type: CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.repartidores
    ADD CONSTRAINT repartidores_pkey PRIMARY KEY (id);


--
-- Name: repartidores repartidores_usuario_id_key; Type: CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.repartidores
    ADD CONSTRAINT repartidores_usuario_id_key UNIQUE (usuario_id);


--
-- Name: rutas_cache rutas_cache_pkey; Type: CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.rutas_cache
    ADD CONSTRAINT rutas_cache_pkey PRIMARY KEY (id);


--
-- Name: paradas uq_parada_recorrido_asignacion; Type: CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.paradas
    ADD CONSTRAINT uq_parada_recorrido_asignacion UNIQUE (recorrido_id, asignacion_id);


--
-- Name: anulaciones anulaciones_pkey; Type: CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.anulaciones
    ADD CONSTRAINT anulaciones_pkey PRIMARY KEY (id);


--
-- Name: comanda_lineas comanda_lineas_pkey; Type: CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.comanda_lineas
    ADD CONSTRAINT comanda_lineas_pkey PRIMARY KEY (id);


--
-- Name: comandas comandas_pkey; Type: CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.comandas
    ADD CONSTRAINT comandas_pkey PRIMARY KEY (id);


--
-- Name: confirmaciones confirmaciones_pedido_id_idempotency_key_key; Type: CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.confirmaciones
    ADD CONSTRAINT confirmaciones_pedido_id_idempotency_key_key UNIQUE (pedido_id, idempotency_key);


--
-- Name: confirmaciones confirmaciones_pkey; Type: CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.confirmaciones
    ADD CONSTRAINT confirmaciones_pkey PRIMARY KEY (id);


--
-- Name: cuentas cuentas_pkey; Type: CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.cuentas
    ADD CONSTRAINT cuentas_pkey PRIMARY KEY (id);


--
-- Name: pedido_linea_extras pedido_linea_extras_pkey; Type: CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedido_linea_extras
    ADD CONSTRAINT pedido_linea_extras_pkey PRIMARY KEY (id);


--
-- Name: pedido_linea_ingredientes pedido_linea_ingredientes_pkey; Type: CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedido_linea_ingredientes
    ADD CONSTRAINT pedido_linea_ingredientes_pkey PRIMARY KEY (id);


--
-- Name: pedido_lineas pedido_lineas_pkey; Type: CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedido_lineas
    ADD CONSTRAINT pedido_lineas_pkey PRIMARY KEY (id);


--
-- Name: pedidos pedidos_codigo_key; Type: CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedidos
    ADD CONSTRAINT pedidos_codigo_key UNIQUE (codigo);


--
-- Name: pedidos pedidos_pkey; Type: CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedidos
    ADD CONSTRAINT pedidos_pkey PRIMARY KEY (id);


--
-- Name: categorias_roles categorias_roles_codigo_key; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.categorias_roles
    ADD CONSTRAINT categorias_roles_codigo_key UNIQUE (codigo);


--
-- Name: categorias_roles categorias_roles_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.categorias_roles
    ADD CONSTRAINT categorias_roles_pkey PRIMARY KEY (id);


--
-- Name: permisos permisos_codigo_key; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.permisos
    ADD CONSTRAINT permisos_codigo_key UNIQUE (codigo);


--
-- Name: permisos permisos_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.permisos
    ADD CONSTRAINT permisos_pkey PRIMARY KEY (id);


--
-- Name: recuperacion_password recuperacion_password_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.recuperacion_password
    ADD CONSTRAINT recuperacion_password_pkey PRIMARY KEY (id);


--
-- Name: recuperacion_password recuperacion_password_token_hash_key; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.recuperacion_password
    ADD CONSTRAINT recuperacion_password_token_hash_key UNIQUE (token_hash);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_token_hash_key; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_hash_key UNIQUE (token_hash);


--
-- Name: roles roles_codigo_key; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.roles
    ADD CONSTRAINT roles_codigo_key UNIQUE (codigo);


--
-- Name: roles_permisos roles_permisos_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.roles_permisos
    ADD CONSTRAINT roles_permisos_pkey PRIMARY KEY (rol_id, permiso_id);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id);


--
-- Name: usuarios_permisos usuarios_permisos_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.usuarios_permisos
    ADD CONSTRAINT usuarios_permisos_pkey PRIMARY KEY (usuario_id, permiso_id);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- Name: usuarios usuarios_username_key; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.usuarios
    ADD CONSTRAINT usuarios_username_key UNIQUE (username);


--
-- Name: verificacion_email verificacion_email_pkey; Type: CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.verificacion_email
    ADD CONSTRAINT verificacion_email_pkey PRIMARY KEY (id);


--
-- Name: caja_movimientos caja_movimientos_pkey; Type: CONSTRAINT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.caja_movimientos
    ADD CONSTRAINT caja_movimientos_pkey PRIMARY KEY (id);


--
-- Name: cajas cajas_pkey; Type: CONSTRAINT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.cajas
    ADD CONSTRAINT cajas_pkey PRIMARY KEY (id);


--
-- Name: comprobantes comprobantes_correlativo_key; Type: CONSTRAINT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.comprobantes
    ADD CONSTRAINT comprobantes_correlativo_key UNIQUE (correlativo);


--
-- Name: comprobantes comprobantes_pkey; Type: CONSTRAINT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.comprobantes
    ADD CONSTRAINT comprobantes_pkey PRIMARY KEY (id);


--
-- Name: comprobantes comprobantes_secuencial_key; Type: CONSTRAINT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.comprobantes
    ADD CONSTRAINT comprobantes_secuencial_key UNIQUE (secuencial);


--
-- Name: pagos pagos_pkey; Type: CONSTRAINT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.pagos
    ADD CONSTRAINT pagos_pkey PRIMARY KEY (id);


--
-- Name: archivos_limpieza_idx; Type: INDEX; Schema: archivos; Owner: -
--

CREATE INDEX archivos_limpieza_idx ON archivos.archivos USING btree (estado, modificado_en);


--
-- Name: idx_auditoria_categoria_fecha; Type: INDEX; Schema: auditoria; Owner: -
--

CREATE INDEX idx_auditoria_categoria_fecha ON auditoria.auditoria_eventos USING btree (categoria, fecha DESC);


--
-- Name: idx_auditoria_entidad; Type: INDEX; Schema: auditoria; Owner: -
--

CREATE INDEX idx_auditoria_entidad ON auditoria.auditoria_eventos USING btree (entidad, entidad_id);


--
-- Name: idx_auditoria_fecha; Type: INDEX; Schema: auditoria; Owner: -
--

CREATE INDEX idx_auditoria_fecha ON auditoria.auditoria_eventos USING btree (fecha);


--
-- Name: idx_auditoria_ip_fecha; Type: INDEX; Schema: auditoria; Owner: -
--

CREATE INDEX idx_auditoria_ip_fecha ON auditoria.auditoria_eventos USING btree (ip, fecha DESC);


--
-- Name: idx_auditoria_tipo_fecha; Type: INDEX; Schema: auditoria; Owner: -
--

CREATE INDEX idx_auditoria_tipo_fecha ON auditoria.auditoria_eventos USING btree (tipo, fecha DESC);


--
-- Name: idx_auditoria_usuario_fecha; Type: INDEX; Schema: auditoria; Owner: -
--

CREATE INDEX idx_auditoria_usuario_fecha ON auditoria.auditoria_eventos USING btree (usuario, fecha DESC);


--
-- Name: idx_outbox_estado; Type: INDEX; Schema: auditoria; Owner: -
--

CREATE INDEX idx_outbox_estado ON auditoria.outbox USING btree (estado);


--
-- Name: idx_mesas_zona; Type: INDEX; Schema: catalogo; Owner: -
--

CREATE INDEX idx_mesas_zona ON catalogo.mesas USING btree (zona_id);


--
-- Name: idx_producto_ingredientes_producto; Type: INDEX; Schema: catalogo; Owner: -
--

CREATE INDEX idx_producto_ingredientes_producto ON catalogo.producto_ingredientes USING btree (producto_id);


--
-- Name: idx_productos_area; Type: INDEX; Schema: catalogo; Owner: -
--

CREATE INDEX idx_productos_area ON catalogo.productos USING btree (area_id);


--
-- Name: idx_productos_categoria; Type: INDEX; Schema: catalogo; Owner: -
--

CREATE INDEX idx_productos_categoria ON catalogo.productos USING btree (categoria_id);


--
-- Name: uq_zonas_nombre; Type: INDEX; Schema: catalogo; Owner: -
--

CREATE UNIQUE INDEX uq_zonas_nombre ON catalogo.zonas USING btree (lower((nombre)::text));


--
-- Name: idx_metodos_pago_cliente; Type: INDEX; Schema: comercial; Owner: -
--

CREATE INDEX idx_metodos_pago_cliente ON comercial.metodos_pago_cliente USING btree (cliente_id);


--
-- Name: idx_pedcli_cliente; Type: INDEX; Schema: comercial; Owner: -
--

CREATE INDEX idx_pedcli_cliente ON comercial.pedidos_clientes USING btree (cliente_id);


--
-- Name: idx_pedcli_estado; Type: INDEX; Schema: comercial; Owner: -
--

CREATE INDEX idx_pedcli_estado ON comercial.pedidos_clientes USING btree (estado);


--
-- Name: idx_asignaciones_recorrido; Type: INDEX; Schema: delivery; Owner: -
--

CREATE INDEX idx_asignaciones_recorrido ON delivery.asignaciones USING btree (recorrido_id);


--
-- Name: idx_asignaciones_repartidor; Type: INDEX; Schema: delivery; Owner: -
--

CREATE INDEX idx_asignaciones_repartidor ON delivery.asignaciones USING btree (repartidor_id);


--
-- Name: idx_incidencias_parada; Type: INDEX; Schema: delivery; Owner: -
--

CREATE INDEX idx_incidencias_parada ON delivery.incidencias USING btree (parada_id);


--
-- Name: idx_paradas_recorrido; Type: INDEX; Schema: delivery; Owner: -
--

CREATE INDEX idx_paradas_recorrido ON delivery.paradas USING btree (recorrido_id);


--
-- Name: idx_recorridos_repartidor; Type: INDEX; Schema: delivery; Owner: -
--

CREATE INDEX idx_recorridos_repartidor ON delivery.recorridos USING btree (repartidor_id);


--
-- Name: uq_asignacion_activa_por_pedido; Type: INDEX; Schema: delivery; Owner: -
--

CREATE UNIQUE INDEX uq_asignacion_activa_por_pedido ON delivery.asignaciones USING btree (pedido_cliente_id) WHERE ((estado)::text <> ALL ((ARRAY['REASIGNADA'::character varying, 'FALLIDA'::character varying])::text[]));


--
-- Name: uq_config_restaurante_fila_unica; Type: INDEX; Schema: delivery; Owner: -
--

CREATE UNIQUE INDEX uq_config_restaurante_fila_unica ON delivery.config_restaurante USING btree (fila_unica);


--
-- Name: uq_rutas_cache_clave; Type: INDEX; Schema: delivery; Owner: -
--

CREATE UNIQUE INDEX uq_rutas_cache_clave ON delivery.rutas_cache USING btree (clave_hash);


--
-- Name: idx_anulaciones_estado; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE INDEX idx_anulaciones_estado ON operaciones.anulaciones USING btree (estado);


--
-- Name: idx_anulaciones_pedido; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE INDEX idx_anulaciones_pedido ON operaciones.anulaciones USING btree (pedido_codigo);


--
-- Name: idx_comanda_lineas_comanda; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE INDEX idx_comanda_lineas_comanda ON operaciones.comanda_lineas USING btree (comanda_id);


--
-- Name: idx_comandas_estado; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE INDEX idx_comandas_estado ON operaciones.comandas USING btree (estado);


--
-- Name: idx_comandas_pedido; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE INDEX idx_comandas_pedido ON operaciones.comandas USING btree (pedido_codigo);


--
-- Name: idx_confirmaciones_pedido; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE INDEX idx_confirmaciones_pedido ON operaciones.confirmaciones USING btree (pedido_id);


--
-- Name: idx_linea_extras_linea; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE INDEX idx_linea_extras_linea ON operaciones.pedido_linea_extras USING btree (linea_id);


--
-- Name: idx_lineas_pedido; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE INDEX idx_lineas_pedido ON operaciones.pedido_lineas USING btree (pedido_id);


--
-- Name: idx_pedidos_cuenta; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE INDEX idx_pedidos_cuenta ON operaciones.pedidos USING btree (cuenta_id);


--
-- Name: idx_pedidos_estado; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE INDEX idx_pedidos_estado ON operaciones.pedidos USING btree (estado);


--
-- Name: idx_pedidos_mesa; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE INDEX idx_pedidos_mesa ON operaciones.pedidos USING btree (mesa_id);


--
-- Name: uq_comanda_anulacion; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE UNIQUE INDEX uq_comanda_anulacion ON operaciones.comandas USING btree (anulacion_id) WHERE (anulacion_id IS NOT NULL);


--
-- Name: uq_comanda_pedido_area_orden; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE UNIQUE INDEX uq_comanda_pedido_area_orden ON operaciones.comandas USING btree (pedido_codigo, area_id) WHERE ((tipo)::text = 'ORDEN'::text);


--
-- Name: uq_cuenta_mesa_abierta; Type: INDEX; Schema: operaciones; Owner: -
--

CREATE UNIQUE INDEX uq_cuenta_mesa_abierta ON operaciones.cuentas USING btree (mesa_id) WHERE ((estado)::text = 'ABIERTA'::text);


--
-- Name: idx_recuperacion_password_usuario; Type: INDEX; Schema: seguridad; Owner: -
--

CREATE INDEX idx_recuperacion_password_usuario ON seguridad.recuperacion_password USING btree (usuario_id);


--
-- Name: idx_refresh_tokens_usuario; Type: INDEX; Schema: seguridad; Owner: -
--

CREATE INDEX idx_refresh_tokens_usuario ON seguridad.refresh_tokens USING btree (usuario_id);


--
-- Name: idx_usuarios_permisos_usuario; Type: INDEX; Schema: seguridad; Owner: -
--

CREATE INDEX idx_usuarios_permisos_usuario ON seguridad.usuarios_permisos USING btree (usuario_id);


--
-- Name: idx_usuarios_rol; Type: INDEX; Schema: seguridad; Owner: -
--

CREATE INDEX idx_usuarios_rol ON seguridad.usuarios USING btree (rol_id);


--
-- Name: idx_verificacion_email_usuario; Type: INDEX; Schema: seguridad; Owner: -
--

CREATE INDEX idx_verificacion_email_usuario ON seguridad.verificacion_email USING btree (usuario_id);


--
-- Name: idx_caja_movimientos_caja; Type: INDEX; Schema: tesoreria; Owner: -
--

CREATE INDEX idx_caja_movimientos_caja ON tesoreria.caja_movimientos USING btree (caja_id);


--
-- Name: idx_comprobantes_cuenta; Type: INDEX; Schema: tesoreria; Owner: -
--

CREATE INDEX idx_comprobantes_cuenta ON tesoreria.comprobantes USING btree (cuenta_id);


--
-- Name: idx_comprobantes_fecha; Type: INDEX; Schema: tesoreria; Owner: -
--

CREATE INDEX idx_comprobantes_fecha ON tesoreria.comprobantes USING btree (fecha);


--
-- Name: idx_pagos_caja; Type: INDEX; Schema: tesoreria; Owner: -
--

CREATE INDEX idx_pagos_caja ON tesoreria.pagos USING btree (caja_id);


--
-- Name: idx_pagos_cuenta; Type: INDEX; Schema: tesoreria; Owner: -
--

CREATE INDEX idx_pagos_cuenta ON tesoreria.pagos USING btree (cuenta_id);


--
-- Name: uq_caja_abierta; Type: INDEX; Schema: tesoreria; Owner: -
--

CREATE UNIQUE INDEX uq_caja_abierta ON tesoreria.cajas USING btree (estado) WHERE ((estado)::text = 'ABIERTA'::text);


--
-- Name: permisos trg_admin_todos_los_permisos; Type: TRIGGER; Schema: seguridad; Owner: -
--

CREATE TRIGGER trg_admin_todos_los_permisos AFTER INSERT OR UPDATE ON seguridad.permisos FOR EACH ROW EXECUTE FUNCTION seguridad.fn_admin_todos_los_permisos();


--
-- Name: mesas mesas_zona_id_fkey; Type: FK CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.mesas
    ADD CONSTRAINT mesas_zona_id_fkey FOREIGN KEY (zona_id) REFERENCES catalogo.zonas(id);


--
-- Name: producto_extras producto_extras_extra_id_fkey; Type: FK CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.producto_extras
    ADD CONSTRAINT producto_extras_extra_id_fkey FOREIGN KEY (extra_id) REFERENCES catalogo.extras(id) ON DELETE CASCADE;


--
-- Name: producto_extras producto_extras_producto_id_fkey; Type: FK CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.producto_extras
    ADD CONSTRAINT producto_extras_producto_id_fkey FOREIGN KEY (producto_id) REFERENCES catalogo.productos(id) ON DELETE CASCADE;


--
-- Name: producto_ingredientes producto_ingredientes_producto_id_fkey; Type: FK CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.producto_ingredientes
    ADD CONSTRAINT producto_ingredientes_producto_id_fkey FOREIGN KEY (producto_id) REFERENCES catalogo.productos(id) ON DELETE CASCADE;


--
-- Name: productos productos_area_id_fkey; Type: FK CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.productos
    ADD CONSTRAINT productos_area_id_fkey FOREIGN KEY (area_id) REFERENCES catalogo.areas(id);


--
-- Name: productos productos_categoria_id_fkey; Type: FK CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.productos
    ADD CONSTRAINT productos_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES catalogo.categorias(id);


--
-- Name: productos productos_imagen_archivo_id_fkey; Type: FK CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.productos
    ADD CONSTRAINT productos_imagen_archivo_id_fkey FOREIGN KEY (imagen_archivo_id) REFERENCES archivos.archivos(id);


--
-- Name: productos productos_video_archivo_id_fkey; Type: FK CONSTRAINT; Schema: catalogo; Owner: -
--

ALTER TABLE ONLY catalogo.productos
    ADD CONSTRAINT productos_video_archivo_id_fkey FOREIGN KEY (video_archivo_id) REFERENCES archivos.archivos(id);


--
-- Name: clientes_direcciones clientes_direcciones_cliente_id_fkey; Type: FK CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.clientes_direcciones
    ADD CONSTRAINT clientes_direcciones_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES comercial.clientes(id);


--
-- Name: clientes clientes_usuario_id_fkey; Type: FK CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.clientes
    ADD CONSTRAINT clientes_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES seguridad.usuarios(id);


--
-- Name: metodos_pago_cliente metodos_pago_cliente_cliente_id_fkey; Type: FK CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.metodos_pago_cliente
    ADD CONSTRAINT metodos_pago_cliente_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES comercial.clientes(id);


--
-- Name: pedidos_clientes pedidos_clientes_cliente_id_fkey; Type: FK CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes
    ADD CONSTRAINT pedidos_clientes_cliente_id_fkey FOREIGN KEY (cliente_id) REFERENCES comercial.clientes(id);


--
-- Name: pedidos_clientes pedidos_clientes_direccion_id_fkey; Type: FK CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes
    ADD CONSTRAINT pedidos_clientes_direccion_id_fkey FOREIGN KEY (direccion_id) REFERENCES comercial.clientes_direcciones(id);


--
-- Name: pedidos_clientes_lineas_extras pedidos_clientes_lineas_extras_extra_id_fkey; Type: FK CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes_lineas_extras
    ADD CONSTRAINT pedidos_clientes_lineas_extras_extra_id_fkey FOREIGN KEY (extra_id) REFERENCES catalogo.extras(id);


--
-- Name: pedidos_clientes_lineas_extras pedidos_clientes_lineas_extras_linea_id_fkey; Type: FK CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes_lineas_extras
    ADD CONSTRAINT pedidos_clientes_lineas_extras_linea_id_fkey FOREIGN KEY (linea_id) REFERENCES comercial.pedidos_clientes_lineas(id);


--
-- Name: pedidos_clientes_lineas pedidos_clientes_lineas_pedido_id_fkey; Type: FK CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes_lineas
    ADD CONSTRAINT pedidos_clientes_lineas_pedido_id_fkey FOREIGN KEY (pedido_id) REFERENCES comercial.pedidos_clientes(id);


--
-- Name: pedidos_clientes_lineas pedidos_clientes_lineas_producto_id_fkey; Type: FK CONSTRAINT; Schema: comercial; Owner: -
--

ALTER TABLE ONLY comercial.pedidos_clientes_lineas
    ADD CONSTRAINT pedidos_clientes_lineas_producto_id_fkey FOREIGN KEY (producto_id) REFERENCES catalogo.productos(id);


--
-- Name: asignaciones asignaciones_pedido_cliente_id_fkey; Type: FK CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.asignaciones
    ADD CONSTRAINT asignaciones_pedido_cliente_id_fkey FOREIGN KEY (pedido_cliente_id) REFERENCES comercial.pedidos_clientes(id);


--
-- Name: asignaciones asignaciones_recorrido_id_fkey; Type: FK CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.asignaciones
    ADD CONSTRAINT asignaciones_recorrido_id_fkey FOREIGN KEY (recorrido_id) REFERENCES delivery.recorridos(id);


--
-- Name: asignaciones asignaciones_repartidor_id_fkey; Type: FK CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.asignaciones
    ADD CONSTRAINT asignaciones_repartidor_id_fkey FOREIGN KEY (repartidor_id) REFERENCES delivery.repartidores(id);


--
-- Name: geocoding_cache geocoding_cache_direccion_id_fkey; Type: FK CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.geocoding_cache
    ADD CONSTRAINT geocoding_cache_direccion_id_fkey FOREIGN KEY (direccion_id) REFERENCES comercial.clientes_direcciones(id);


--
-- Name: incidencias incidencias_parada_id_fkey; Type: FK CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.incidencias
    ADD CONSTRAINT incidencias_parada_id_fkey FOREIGN KEY (parada_id) REFERENCES delivery.paradas(id);


--
-- Name: paradas paradas_asignacion_id_fkey; Type: FK CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.paradas
    ADD CONSTRAINT paradas_asignacion_id_fkey FOREIGN KEY (asignacion_id) REFERENCES delivery.asignaciones(id);


--
-- Name: paradas paradas_recorrido_id_fkey; Type: FK CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.paradas
    ADD CONSTRAINT paradas_recorrido_id_fkey FOREIGN KEY (recorrido_id) REFERENCES delivery.recorridos(id);


--
-- Name: recorridos recorridos_repartidor_id_fkey; Type: FK CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.recorridos
    ADD CONSTRAINT recorridos_repartidor_id_fkey FOREIGN KEY (repartidor_id) REFERENCES delivery.repartidores(id);


--
-- Name: repartidores repartidores_usuario_id_fkey; Type: FK CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.repartidores
    ADD CONSTRAINT repartidores_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES seguridad.usuarios(id);


--
-- Name: rutas_cache rutas_cache_recorrido_id_fkey; Type: FK CONSTRAINT; Schema: delivery; Owner: -
--

ALTER TABLE ONLY delivery.rutas_cache
    ADD CONSTRAINT rutas_cache_recorrido_id_fkey FOREIGN KEY (recorrido_id) REFERENCES delivery.recorridos(id);


--
-- Name: anulaciones anulaciones_area_id_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.anulaciones
    ADD CONSTRAINT anulaciones_area_id_fkey FOREIGN KEY (area_id) REFERENCES catalogo.areas(id);


--
-- Name: anulaciones anulaciones_pedido_codigo_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.anulaciones
    ADD CONSTRAINT anulaciones_pedido_codigo_fkey FOREIGN KEY (pedido_codigo) REFERENCES operaciones.pedidos(codigo);


--
-- Name: anulaciones anulaciones_producto_id_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.anulaciones
    ADD CONSTRAINT anulaciones_producto_id_fkey FOREIGN KEY (producto_id) REFERENCES catalogo.productos(id);


--
-- Name: comanda_lineas comanda_lineas_comanda_id_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.comanda_lineas
    ADD CONSTRAINT comanda_lineas_comanda_id_fkey FOREIGN KEY (comanda_id) REFERENCES operaciones.comandas(id) ON DELETE CASCADE;


--
-- Name: comandas comandas_anulacion_id_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.comandas
    ADD CONSTRAINT comandas_anulacion_id_fkey FOREIGN KEY (anulacion_id) REFERENCES operaciones.anulaciones(id);


--
-- Name: comandas comandas_area_id_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.comandas
    ADD CONSTRAINT comandas_area_id_fkey FOREIGN KEY (area_id) REFERENCES catalogo.areas(id);


--
-- Name: confirmaciones confirmaciones_pedido_id_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.confirmaciones
    ADD CONSTRAINT confirmaciones_pedido_id_fkey FOREIGN KEY (pedido_id) REFERENCES operaciones.pedidos(id) ON DELETE CASCADE;


--
-- Name: cuentas cuentas_mesa_id_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.cuentas
    ADD CONSTRAINT cuentas_mesa_id_fkey FOREIGN KEY (mesa_id) REFERENCES catalogo.mesas(id);


--
-- Name: pedido_linea_extras pedido_linea_extras_linea_id_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedido_linea_extras
    ADD CONSTRAINT pedido_linea_extras_linea_id_fkey FOREIGN KEY (linea_id) REFERENCES operaciones.pedido_lineas(id) ON DELETE CASCADE;


--
-- Name: pedido_linea_ingredientes pedido_linea_ingredientes_linea_id_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedido_linea_ingredientes
    ADD CONSTRAINT pedido_linea_ingredientes_linea_id_fkey FOREIGN KEY (linea_id) REFERENCES operaciones.pedido_lineas(id) ON DELETE CASCADE;


--
-- Name: pedido_lineas pedido_lineas_pedido_id_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedido_lineas
    ADD CONSTRAINT pedido_lineas_pedido_id_fkey FOREIGN KEY (pedido_id) REFERENCES operaciones.pedidos(id) ON DELETE CASCADE;


--
-- Name: pedidos pedidos_cuenta_id_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedidos
    ADD CONSTRAINT pedidos_cuenta_id_fkey FOREIGN KEY (cuenta_id) REFERENCES operaciones.cuentas(id);


--
-- Name: pedidos pedidos_mesa_id_fkey; Type: FK CONSTRAINT; Schema: operaciones; Owner: -
--

ALTER TABLE ONLY operaciones.pedidos
    ADD CONSTRAINT pedidos_mesa_id_fkey FOREIGN KEY (mesa_id) REFERENCES catalogo.mesas(id);


--
-- Name: recuperacion_password recuperacion_password_usuario_id_fkey; Type: FK CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.recuperacion_password
    ADD CONSTRAINT recuperacion_password_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES seguridad.usuarios(id) ON DELETE CASCADE;


--
-- Name: refresh_tokens refresh_tokens_usuario_id_fkey; Type: FK CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.refresh_tokens
    ADD CONSTRAINT refresh_tokens_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES seguridad.usuarios(id) ON DELETE CASCADE;


--
-- Name: roles roles_categoria_id_fkey; Type: FK CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.roles
    ADD CONSTRAINT roles_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES seguridad.categorias_roles(id);


--
-- Name: roles_permisos roles_permisos_permiso_id_fkey; Type: FK CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.roles_permisos
    ADD CONSTRAINT roles_permisos_permiso_id_fkey FOREIGN KEY (permiso_id) REFERENCES seguridad.permisos(id) ON DELETE CASCADE;


--
-- Name: roles_permisos roles_permisos_rol_id_fkey; Type: FK CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.roles_permisos
    ADD CONSTRAINT roles_permisos_rol_id_fkey FOREIGN KEY (rol_id) REFERENCES seguridad.roles(id) ON DELETE CASCADE;


--
-- Name: usuarios_permisos usuarios_permisos_permiso_id_fkey; Type: FK CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.usuarios_permisos
    ADD CONSTRAINT usuarios_permisos_permiso_id_fkey FOREIGN KEY (permiso_id) REFERENCES seguridad.permisos(id) ON DELETE CASCADE;


--
-- Name: usuarios_permisos usuarios_permisos_usuario_id_fkey; Type: FK CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.usuarios_permisos
    ADD CONSTRAINT usuarios_permisos_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES seguridad.usuarios(id) ON DELETE CASCADE;


--
-- Name: usuarios usuarios_rol_id_fkey; Type: FK CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.usuarios
    ADD CONSTRAINT usuarios_rol_id_fkey FOREIGN KEY (rol_id) REFERENCES seguridad.roles(id);


--
-- Name: verificacion_email verificacion_email_usuario_id_fkey; Type: FK CONSTRAINT; Schema: seguridad; Owner: -
--

ALTER TABLE ONLY seguridad.verificacion_email
    ADD CONSTRAINT verificacion_email_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES seguridad.usuarios(id) ON DELETE CASCADE;


--
-- Name: caja_movimientos caja_movimientos_caja_id_fkey; Type: FK CONSTRAINT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.caja_movimientos
    ADD CONSTRAINT caja_movimientos_caja_id_fkey FOREIGN KEY (caja_id) REFERENCES tesoreria.cajas(id);


--
-- Name: caja_movimientos caja_movimientos_pago_id_fkey; Type: FK CONSTRAINT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.caja_movimientos
    ADD CONSTRAINT caja_movimientos_pago_id_fkey FOREIGN KEY (pago_id) REFERENCES tesoreria.pagos(id);


--
-- Name: comprobantes comprobantes_cuenta_id_fkey; Type: FK CONSTRAINT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.comprobantes
    ADD CONSTRAINT comprobantes_cuenta_id_fkey FOREIGN KEY (cuenta_id) REFERENCES operaciones.cuentas(id);


--
-- Name: pagos pagos_caja_id_fkey; Type: FK CONSTRAINT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.pagos
    ADD CONSTRAINT pagos_caja_id_fkey FOREIGN KEY (caja_id) REFERENCES tesoreria.cajas(id);


--
-- Name: pagos pagos_cuenta_id_fkey; Type: FK CONSTRAINT; Schema: tesoreria; Owner: -
--

ALTER TABLE ONLY tesoreria.pagos
    ADD CONSTRAINT pagos_cuenta_id_fkey FOREIGN KEY (cuenta_id) REFERENCES operaciones.cuentas(id);


--
-- PostgreSQL database dump complete
--


