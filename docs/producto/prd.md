# PRD — Sistema de Gestión de Restaurante

## Propósito observado

El producto reúne herramientas para administrar la operación de un restaurante: usuarios y permisos, catálogo, mesas, pedidos, comandas, cuentas, pagos, facturación, caja, reportes y auditoría. Ofrece API Java/Spring, interfaz web Next.js y aplicación móvil Expo.

## Capacidades implementadas

- Administrar usuarios, roles, permisos, autenticación y verificación de correo.
- Configurar productos, categorías, áreas de preparación y parámetros.
- Gestionar mesas y pedidos presenciales; generar comandas y seguir su preparación.
- Identificar las mesas con códigos visibles como `M01` en el panel web; al crear una mesa,
  proponer un código libre y señalar si ya existe, incluso cuando está deshabilitada. Mostrar
  ese código en pedidos, cuentas y dashboards; la deshabilitación conserva el registro y el
  número para poder reactivarlo.
- Atender pedidos de clientes, perfiles, direcciones e historial desde las capacidades de clientes.
- Gestionar anulaciones, cuentas, pagos, comprobantes y sesiones/movimientos de caja.
- Consultar auditoría, resumen financiero y reportes disponibles.

El alcance exacto de cada endpoint está en [API](../api/README.md) y las responsabilidades por módulo en [módulos](../modulos/README.md).

## Interfaces

- Web de **administración y caja** (solo `ADMIN` y `CAJERO`) con vistas de login, dashboard, mesas y zonas, monitor de pedidos y de comandas, caja, cuentas, productos, usuarios, anulaciones, entregas, auditoría y reportes.
- Aplicación móvil Expo con flujos de cliente, mesero, cocina y repartidor documentados en `mobile/README.md`. La matriz de quién hace qué está en [canales y roles](canales-y-roles.md).
- API REST versionada bajo `/api/v1`; incluye comunicación en tiempo real descrita por la documentación API.

## Requisitos no funcionales observados

El backend valida esquema con Hibernate y aplica migraciones Flyway. Se usan cantidades monetarias `BigDecimal`; la configuración define UTC y redondeo `HALF_UP`. La seguridad usa JWT y autorización por roles/permisos. TLS para PostgreSQL es el valor predeterminado de conexión.

## Pendiente de definición por producto

No se encontraron objetivos cuantificados, perfiles de usuario formalizados, criterios de aceptación por capacidad, política de retención, disponibilidad/SLA ni prioridades de roadmap verificables. Deben acordarse con responsables del producto.

El límite de 1 a 99 está aplicado al formulario web de mesas. Está pendiente decidir si debe
ser una regla de negocio general de la API y cómo tratar nuevos registros fuera de ese rango
creados por otros clientes.
