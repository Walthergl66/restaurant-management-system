# Módulos y funciones

Inventario funcional derivado de los paquetes del backend, rutas de la web y documentación de componentes. No sustituye los permisos efectivos definidos por la API.

## API

El backend es un monolito modular Spring Modulith bajo `com.restaurante`.

| Módulo | Responsabilidad observada |
|---|---|
| `usuarios` | Autenticación, registro, usuarios, roles/permisos, JWT, verificación y recuperación de cuenta, correo transaccional. |
| `clientes` | Integraciones SPI para perfil, direcciones, carrito y métodos de pago de clientes. |
| `catalogo` | Productos, categorías y datos de catálogo. |
| `configuracion` | Parámetros operativos e impresoras. |
| `mesas` | Consulta y gestión de mesas y su habilitación. |
| `pedidos` | Creación y modificación de pedidos/líneas, confirmación y cancelación. |
| `comandas` | Generación y seguimiento de comandas de preparación, impresión y errores de impresión. |
| `cuentas` | Cuenta asociada a operación, adiciones, cálculo de importes y cierre. |
| `pagos` | Cobro de cuentas y registro de medios/documentos de pago. |
| `facturacion` | Emisión y consulta de comprobantes. |
| `caja` | Apertura, movimientos, egresos y cierre de caja. |
| `anulaciones` | Solicitud y gestión de anulaciones. |
| `reportes` | Consultas y reportes operativos/financieros. |
| `finanzas` | Consultas financieras. |
| `auditoria` | Registro y consulta de eventos auditables. |

## Aplicaciones cliente

- **Web** (`frontend/src/app`, `frontend/src/features`): acceso, dashboard, mesas, pedidos, cocina/KDS, cuentas, caja, productos, usuarios, auditoría, anulaciones y reportes. Next.js enruta `/api/*` al backend.
- **Móvil** (`mobile/app`, `mobile/src`): flujos de cliente para menú, carrito, checkout, pedidos, direcciones y medios de pago; y atención de mesas para meseros, según el README móvil.
- **Base de datos** (`backend/src/main/resources/db/migration`): migraciones Flyway; scripts de respaldo/restauración en `database/`.

## Responsabilidades transversales

Seguridad y autorización se implementan en `usuarios` y configuración Spring Security. Persistencia se organiza mediante repositorios JPA. La web mantiene estado remoto con TanStack Query y estado de interfaz/sesión con Zustand. Correo se integra con Brevo; la configuración de impresoras y los flujos de impresión se reparten entre configuración y comandas.

## Verificación

Los endpoints y permisos publicados se detallan en [API](../api.md). Los módulos deben mantenerse alineados con los paquetes fuente y las pruebas de modularidad; este inventario resume responsabilidades visibles y no especifica reglas de negocio que no estén documentadas.
