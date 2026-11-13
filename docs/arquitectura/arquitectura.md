# Arquitectura del sistema

## Vista general

Monorepo con tres clientes/componentes principales: API Spring Boot modular, aplicaciÃ³n web Next.js y aplicaciÃ³n mÃ³vil Expo. La API persiste en PostgreSQL externo. La web enruta `/api/*` al backend; la app mÃ³vil usa la URL de API configurada para su entorno.

```mermaid
flowchart LR
  Web[Web Next.js] -->|HTTP /api| API[API Spring Boot]
  Mobile[App mÃ³vil Expo] -->|HTTP| API
  API -->|JDBC + Flyway| DB[(PostgreSQL externo)]
  API --> Mail[Brevo]
```

## Backend

El backend implementa un monolito modular con Spring Modulith. Los mÃ³dulos de dominio encapsulan web, aplicaciÃ³n, dominio e infraestructura cuando corresponde. Los contratos HTTP se exponen mediante controladores y DTO; repositorios JPA conectan dominio/aplicaciÃ³n con PostgreSQL. Flyway versiona el esquema. Seguridad basada en Spring Security y JWT reside principalmente en usuarios.

## Clientes

La web utiliza Next.js App Router, mÃ³dulos por feature y un cliente HTTP comÃºn; el rewrite de Next evita que el navegador necesite conocer el host interno del backend. La app mÃ³vil organiza rutas Expo y mÃ³dulos compartidos/funcionales; su direcciÃ³n API se resuelve segÃºn la configuraciÃ³n del cliente.

## Despliegue

Docker Compose ejecuta backend y web. PostgreSQL es un servicio externo y debe estar disponible antes de que arranque la API. La API aplica migraciones al inicio. ProducciÃ³n y variables sensibles se configuran segÃºn [produccion.md](../operacion/despliegue.md).

## LÃ­mites conocidos

La topologÃ­a real de producciÃ³n, proveedor cloud, terminaciÃ³n TLS, dominios, escalado horizontal y alta disponibilidad dependen del entorno y deben confirmarse con su operador.

