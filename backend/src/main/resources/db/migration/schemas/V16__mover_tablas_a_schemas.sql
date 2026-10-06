-- =====================================================================
-- V16__mover_tablas_a_schemas.sql. ADITIVA sobre V1..V15.
--
-- Traslado de las 34 tablas y 2 secuencias desde `public` al schema de su
-- responsabilidad (definidos en V15). Se ejecuta en el mismo despliegue
-- que la actualización de las entidades JPA (@Table schema=...): con
-- `ddl-auto: validate`, BD y entidades deben apuntar al mismo sitio.
--
-- ALTER TABLE ... SET SCHEMA conserva columnas, restricciones, índices,
-- permisos y secuencias vinculadas: solo cambia el namespace. Las FK que
-- cruzan schemas (10 en total) se actualizan solas.
-- =====================================================================

-- 01 seguridad: identidad y autorización
ALTER TABLE public.roles           SET SCHEMA seguridad;
ALTER TABLE public.permisos        SET SCHEMA seguridad;
ALTER TABLE public.roles_permisos  SET SCHEMA seguridad;
ALTER TABLE public.usuarios        SET SCHEMA seguridad;
ALTER TABLE public.refresh_tokens  SET SCHEMA seguridad;

-- 02 catalogo: catálogo y recursos estáticos
ALTER TABLE public.categorias            SET SCHEMA catalogo;
ALTER TABLE public.areas                 SET SCHEMA catalogo;
ALTER TABLE public.extras                SET SCHEMA catalogo;
ALTER TABLE public.productos             SET SCHEMA catalogo;
ALTER TABLE public.producto_extras       SET SCHEMA catalogo;
ALTER TABLE public.producto_ingredientes SET SCHEMA catalogo;
ALTER TABLE public.mesas                 SET SCHEMA catalogo;

-- 03 configuracion: parámetros y dispositivos
ALTER TABLE public.parametros  SET SCHEMA configuracion;
ALTER TABLE public.impresoras  SET SCHEMA configuracion;

-- 04 operaciones: ciclo del servicio en sala
ALTER TABLE public.cuentas                  SET SCHEMA operaciones;
ALTER TABLE public.pedidos                  SET SCHEMA operaciones;
ALTER TABLE public.pedido_lineas            SET SCHEMA operaciones;
ALTER TABLE public.pedido_linea_extras      SET SCHEMA operaciones;
ALTER TABLE public.pedido_linea_ingredientes SET SCHEMA operaciones;
ALTER TABLE public.confirmaciones           SET SCHEMA operaciones;
ALTER TABLE public.anulaciones              SET SCHEMA operaciones;
ALTER TABLE public.comandas                 SET SCHEMA operaciones;
ALTER TABLE public.comanda_lineas           SET SCHEMA operaciones;

ALTER SEQUENCE public.seq_numero_comanda SET SCHEMA operaciones;

-- 05 comercial: canal del cliente final
ALTER TABLE public.clientes                        SET SCHEMA comercial;
ALTER TABLE public.clientes_direcciones            SET SCHEMA comercial;
ALTER TABLE public.pedidos_clientes                SET SCHEMA comercial;
ALTER TABLE public.pedidos_clientes_lineas         SET SCHEMA comercial;
ALTER TABLE public.pedidos_clientes_lineas_extras  SET SCHEMA comercial;

-- 06 tesoreria: dinero
ALTER TABLE public.cajas             SET SCHEMA tesoreria;
ALTER TABLE public.caja_movimientos  SET SCHEMA tesoreria;
ALTER TABLE public.pagos             SET SCHEMA tesoreria;
ALTER TABLE public.comprobantes      SET SCHEMA tesoreria;

ALTER SEQUENCE public.seq_correlativo_comprobante SET SCHEMA tesoreria;

-- 07 auditoria: trazabilidad y outbox
ALTER TABLE public.auditoria_eventos SET SCHEMA auditoria;
ALTER TABLE public.outbox            SET SCHEMA auditoria;
