# PRD — Sistema de Gestión de Restaurante

## Propósito observado

El producto reúne herramientas para administrar la operación de un restaurante: usuarios y permisos, catálogo, mesas, pedidos, comandas, cuentas, pagos, facturación, caja, reportes y auditoría. Ofrece API Java/Spring, interfaz web Next.js y aplicación móvil Expo.

## Capacidades implementadas

- Administrar usuarios, roles, permisos, autenticación y verificación de correo.
- Configurar productos, categorías, áreas de preparación y parámetros.
- Gestionar mesas y pedidos presenciales; generar comandas y seguir su preparación.
- Atender pedidos de clientes, perfiles, direcciones e historial desde las capacidades de clientes.
- Gestionar anulaciones, cuentas, pagos, comprobantes y sesiones/movimientos de caja.
- Consultar auditoría, resumen financiero y reportes disponibles.

El alcance exacto de cada endpoint está en [API](../api.md) y las responsabilidades por módulo en [módulos](../modulos/README.md).

## Interfaces

- Web administrativa/operativa con vistas de login, dashboard, mesas, pedidos, cocina, caja, cuentas, productos, usuarios, anulaciones, auditoría y reportes.
- Aplicación móvil Expo con flujos de cliente y mesero documentados en `mobile/README.md`.
- API REST versionada bajo `/api/v1`; incluye comunicación en tiempo real descrita por la documentación API.

## Requisitos no funcionales observados

El backend valida esquema con Hibernate y aplica migraciones Flyway. Se usan cantidades monetarias `BigDecimal`; la configuración define UTC y redondeo `HALF_UP`. La seguridad usa JWT y autorización por roles/permisos. TLS para PostgreSQL es el valor predeterminado de conexión.

## Pendiente de definición por producto

No se encontraron objetivos cuantificados, perfiles de usuario formalizados, criterios de aceptación por capacidad, política de retención, disponibilidad/SLA ni prioridades de roadmap verificables. Deben acordarse con responsables del producto.
