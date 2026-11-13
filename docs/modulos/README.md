# Módulos y funciones

El sistema cubre operación de salón, pedidos y preparación, cobros, caja, administración y atención digital a clientes. El inventario funcional está organizado por [responsabilidad de módulos](../arquitectura/modulos-y-funciones.md); los módulos del backend se corresponden con paquetes Spring Modulith.

## Capacidades

- **Acceso y administración:** inicio de sesión, usuarios, roles, permisos, verificación de correo y recuperación de contraseña.
- **Catálogo y configuración:** productos, categorías, parámetros e impresoras.
- **Operación de restaurante:** mesas, pedidos, comandas por área de preparación, cuentas y adiciones.
- **Cierre comercial:** cobros, métodos de pago, comprobantes, anulaciones, caja y reportes.
- **Control:** auditoría de eventos.
- **Canales cliente:** web administrativa/operativa y flujos móviles de cliente y mesero.

Los detalles de endpoints y permisos se consultan en [API](../api.md); pantallas y operación por canal están descritas en README de cada componente.
