-- =====================================================================
-- V15__crear_schemas.sql. ADITIVA sobre V1..V14.
--
-- Divide el schema `public` en schemas por responsabilidad. Por ahora solo
-- se CREAN los schemas; el traslado de tablas ocurre en V16 (ALTER TABLE
-- ... SET SCHEMA), que se aplica junto con la actualización de las
-- entidades JPA (@Table schema=...) — `ddl-auto: validate` exige que BD y
-- entidades apunten al mismo sitio en el mismo despliegue.
--
-- Justificación de cada schema:
--   seguridad     Identidad y autorización: usuarios, roles, permisos,
--                 refresh tokens. Es el perímetro de acceso al sistema.
--   catalogo      Catálogo y recursos estáticos: categorías, áreas, extras,
--                 productos, ingredientes y mesas. Es lo más referenciado.
--   configuracion Parámetros clave/valor (IVA, moneda, nombre) y hardware
--                 (impresoras). Leído por muchos módulos, escrito pocas veces.
--   operaciones   Ciclo del servicio en sala: pedidos, líneas congeladas,
--                 confirmaciones, comandas, cuentas y anulaciones.
--   comercial     Canal del cliente final: clientes, direcciones y pedidos
--                 de la app. Canal propio, no mezcla con el servicio en mesa.
--   tesoreria     Dinero: cajas, movimientos, pagos y comprobantes.
--                 Permisos más restrictivos y auditoría sin ruido.
--   auditoria     Historial de solo-inserción y outbox (entrega at-least-once
--                 de impresión y avisos). Si desaparece, el negocio sigue.
--
-- `public` queda para flyway_schema_history.
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS seguridad;
CREATE SCHEMA IF NOT EXISTS catalogo;
CREATE SCHEMA IF NOT EXISTS configuracion;
CREATE SCHEMA IF NOT EXISTS operaciones;
CREATE SCHEMA IF NOT EXISTS comercial;
CREATE SCHEMA IF NOT EXISTS tesoreria;
CREATE SCHEMA IF NOT EXISTS auditoria;

-- La aplicación conecta con el rol `restaurante`: necesita USAGE sobre cada
-- schema para leer y escribir sus tablas. En dev/test el rol de conexión
-- suele ser superusuario (ignora los grants), así que se condiciona a que
-- el rol existe y no es superuser, igual que la REVOKE de V8.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'restaurante'
               AND NOT rolsuper) THEN
        EXECUTE 'GRANT USAGE ON SCHEMA seguridad TO restaurante';
        EXECUTE 'GRANT USAGE ON SCHEMA catalogo TO restaurante';
        EXECUTE 'GRANT USAGE ON SCHEMA configuracion TO restaurante';
        EXECUTE 'GRANT USAGE ON SCHEMA operaciones TO restaurante';
        EXECUTE 'GRANT USAGE ON SCHEMA comercial TO restaurante';
        EXECUTE 'GRANT USAGE ON SCHEMA tesoreria TO restaurante';
        EXECUTE 'GRANT USAGE ON SCHEMA auditoria TO restaurante';
    END IF;
END $$;
