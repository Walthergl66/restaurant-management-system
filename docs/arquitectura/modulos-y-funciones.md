# Módulos y funciones

Inventario funcional derivado de los paquetes del backend, rutas de la web y documentación de componentes. No sustituye los permisos efectivos definidos por la API.

## API

El backend es un monolito modular Spring Modulith bajo `com.restaurante`.

| Módulo | Responsabilidad observada |
|---|---|
| `usuarios` | Inicio/cierre de sesión, renovación de tokens, perfil, registro de clientes y repartidores, verificación de correo, recuperación/cambio de contraseña, administración de usuarios, taxonomía de roles, catálogo y asignación de permisos. Roles y permisos controlan acceso a funciones. Entre los roles asignables está `COCINA`. |
| `clientes` | Perfil del cliente, menú, carrito, pedidos e historial, direcciones y métodos de pago mediante contratos SPI/API cliente. Mesero o cocina entregan en el mostrador los pedidos de retiro. |
| `catalogo` | Categorías, áreas de preparación, productos, extras y menú público consultable, incluso para pedidos desde QR. |
| `configuracion` | Parámetros operativos e impresoras disponibles para la operación. |
| `mesas` | Consulta, creación/edición y habilitación de mesas para el servicio de salón, y gestión de las **zonas** del restaurante (Salón, Terraza…): el administrador las crea, ordena y elimina, y cada mesa se ubica en una. |
| `pedidos` | Crear pedidos, agregar/editar líneas, ingredientes removidos y extras; confirmar, cancelar y avanzar estados de preparación, disponibilidad y entrega/retiro. El mesero marca `ENTREGADO` el pedido de salón servido. |
| `comandas` | Crear comandas por preparación, listar/gestionar estados, recuperar trabajos pendientes de impresión, reenviar impresión y reportar errores. La integración de impresión usa el patrón outbox descrito en documentación técnica. Listado de comandas activas para cocina, con la mesa o la forma de entrega (retiro/domicilio) de cada una; las cancelaciones se dan por enteradas. |
| `cuentas` | Consultar cuentas, añadir consumos, calcular importes con reglas monetarias centralizadas y cerrar cuentas. |
| `pagos` | Registrar cobros de cuentas, líneas y medios de pago/documentos asociados; soporta el flujo de facturación. |
| `facturacion` | Emitir y consultar comprobantes vinculados al cobro mediante el emisor configurado. |
| `caja` | Apertura de caja, registro de movimientos/egresos y cierre con resumen. |
| `anulaciones` | Solicitar y revisar anulaciones sujetas a permisos y al flujo de aprobación de la API. |
| `reportes` | Consultas agregadas para reportes operativos y de ventas. |
| `finanzas` | Consultas financieras asociadas a la operación y sus cobros. |
| `auditoria` | Registrar y consultar eventos auditables con contexto de la petición. |

## Aplicaciones cliente

- **Web** (`frontend/src/app`, `frontend/src/features`): login (solo administrador y cajero), dashboard, mesas y zonas del restaurante, monitor de pedidos y de comandas (solo lectura), cuentas, caja, catálogo de productos, administración de usuarios, auditoría, anulaciones y reportes. El código visible de mesa (`M01`) se deriva de `numero`; en vistas de pedidos se obtiene el número a partir de `mesaId` mediante el catálogo de mesas. El rewrite de Next.js enruta `/api/*` al backend.
- **Móvil** (`mobile/src/app`, `mobile/src/features`): registro/login, verificación y recuperación de cuenta; exploración de menú y producto, carrito, checkout, seguimiento e historial de pedidos, direcciones y métodos de pago; vistas de ayuda, términos y privacidad; atención de mesas y pedidos para meseros (con una pestaña de cocina) pantalla de comandas para el rol cocina y entrega de pedidos listos (mostrador) por mesero y cocina.
- **Base de datos** (`backend/src/main/resources/db/migration`): migraciones Flyway; scripts de respaldo/restauración en `database/`.

## Responsabilidades transversales

Seguridad y autorización se implementan en `usuarios` y configuración Spring Security. Persistencia se organiza mediante repositorios JPA. La web mantiene estado remoto con TanStack Query y estado de interfaz/sesión con Zustand. Correo se integra con Brevo; la configuración de impresoras y los flujos de impresión se reparten entre configuración y comandas.

## Verificación

Los endpoints y permisos publicados se detallan en [API](../api/README.md). Los módulos deben mantenerse alineados con los paquetes fuente y las pruebas de modularidad; este inventario resume responsabilidades visibles y no especifica reglas de negocio que no estén documentadas.
