# Módulos y funciones

Inventario funcional derivado de los paquetes del backend, rutas de la web y documentación de componentes. No sustituye los permisos efectivos definidos por la API.

## API

El backend es un monolito modular Spring Modulith bajo `com.restaurante`.

| Módulo | Responsabilidad observada |
|---|---|
| `usuarios` | Inicio/cierre de sesión, renovación de tokens, perfil, registro de clientes y repartidores, verificación de correo, recuperación/cambio de contraseña, administración de usuarios, taxonomía de roles, catálogo y asignación de permisos. Roles y permisos controlan acceso a funciones. |
| `clientes` | Perfil del cliente, menú, carrito, pedidos e historial, direcciones y métodos de pago mediante contratos SPI/API cliente. |
| `catalogo` | Categorías, áreas de preparación, productos, extras y menú público consultable, incluso para pedidos desde QR. |
| `configuracion` | Parámetros operativos e impresoras disponibles para la operación. |
| `mesas` | Consulta, creación/edición y habilitación de mesas para el servicio de salón. |
| `pedidos` | Crear pedidos, agregar/editar líneas, ingredientes removidos y extras; confirmar, cancelar y avanzar estados de preparación, disponibilidad y entrega/retiro. |
| `comandas` | Crear comandas por preparación, listar/gestionar estados, recuperar trabajos pendientes de impresión, reenviar impresión y reportar errores. La integración de impresión usa el patrón outbox descrito en documentación técnica. |
| `cuentas` | Consultar cuentas, añadir consumos, calcular importes con reglas monetarias centralizadas y cerrar cuentas. |
| `pagos` | Registrar cobros de cuentas, líneas y medios de pago/documentos asociados; soporta el flujo de facturación. |
| `facturacion` | Emitir y consultar comprobantes vinculados al cobro mediante el emisor configurado. |
| `caja` | Apertura de caja, registro de movimientos/egresos y cierre con resumen. |
| `anulaciones` | Solicitar y revisar anulaciones sujetas a permisos y al flujo de aprobación de la API. |
| `reportes` | Consultas agregadas para reportes operativos y de ventas. |
| `finanzas` | Consultas financieras asociadas a la operación y sus cobros. |
| `auditoria` | Registrar y consultar eventos auditables con contexto de la petición. |

## Aplicaciones cliente

- **Web** (`frontend/src/app`, `frontend/src/features`): login, dashboard, mapa/operación de mesas, toma y detalle de pedidos, cocina/KDS, cuentas, caja, catálogo de productos, administración de usuarios, auditoría, anulaciones y reportes. El rewrite de Next.js enruta `/api/*` al backend.
- **Móvil** (`mobile/src/app`, `mobile/src/features`): registro/login, verificación y recuperación de cuenta; exploración de menú y producto, carrito, checkout, seguimiento e historial de pedidos, direcciones y métodos de pago; vistas de ayuda, términos y privacidad; y atención de mesas/pedidos para meseros.
- **Base de datos** (`backend/src/main/resources/db/migration`): migraciones Flyway; scripts de respaldo/restauración en `database/`.

## Responsabilidades transversales

Seguridad y autorización se implementan en `usuarios` y configuración Spring Security. Persistencia se organiza mediante repositorios JPA. La web mantiene estado remoto con TanStack Query y estado de interfaz/sesión con Zustand. Correo se integra con Brevo; la configuración de impresoras y los flujos de impresión se reparten entre configuración y comandas.

## Verificación

Los endpoints y permisos publicados se detallan en [API](../api/README.md). Los módulos deben mantenerse alineados con los paquetes fuente y las pruebas de modularidad; este inventario resume responsabilidades visibles y no especifica reglas de negocio que no estén documentadas.
