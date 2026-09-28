# Guía completa del backend

Esta guía explica cómo funciona, cómo se instala, cómo se inicia, cómo se prueba y cómo se modifica el backend de `restaurant-management-system`. Está pensada para una persona que todavía no conoce Java.

> La fuente de verdad de esta guía es el código actual del repositorio. Cuando existe una diferencia entre documentación antigua y configuración ejecutable, se indica expresamente.

## 1. Resumen del proyecto

El backend es una API REST para administrar la operación completa de un restaurante. Se implementó como un **monolito modular**: se ejecuta como una sola aplicación, pero el código está separado por módulos de negocio.

Incluye:

- usuarios, roles y permisos;
- categorías, productos, extras y áreas de preparación;
- mesas;
- pedidos presenciales;
- pedidos realizados por clientes;
- comandas para cocina;
- trabajos de impresión mediante el patrón outbox;
- cuentas, adiciones y anulaciones;
- caja y movimientos;
- cobros simples o mixtos;
- comprobantes y facturación;
- finanzas, reportes y auditoría;
- comunicación en tiempo real mediante WebSocket/STOMP;
- autenticación con JWT;
- migraciones automáticas de PostgreSQL;
- pruebas unitarias, de integración, concurrencia, WebSocket y carga.

El repositorio es un monorepo. Actualmente el backend está en `backend/`; el frontend y la aplicación móvil todavía no están incluidos.

## 2. Tecnologías utilizadas

| Tecnología | Uso |
|---|---|
| Java 25 | Lenguaje y plataforma de ejecución |
| Spring Boot 4.1 | Base de la aplicación |
| Spring MVC | API HTTP/REST |
| Spring Data JPA + Hibernate | Persistencia y mapeo objeto-relacional |
| PostgreSQL 16 | Base de datos |
| Flyway | Migraciones versionadas de la base |
| Spring Security | Autenticación y autorización |
| JWT/JJWT | Tokens de acceso y refresh |
| Spring Modulith | Verificación de límites entre módulos |
| WebSocket + STOMP | Notificaciones de pedidos en tiempo real |
| springdoc-openapi | Swagger y especificación OpenAPI |
| Maven | Dependencias, compilación y pruebas |
| JUnit 5 | Pruebas |
| Testcontainers | PostgreSQL real y aislado para integración |
| Docker Compose | PostgreSQL para desarrollo local |

Los importes monetarios se representan con `BigDecimal` y con `Money`; nunca deben cambiarse a `double` o `float`.

## 3. Archivos importantes

```text
restaurant-management-system/
├── backend/
│   ├── pom.xml                         Dependencias y build de Maven
│   ├── mvnw / mvnw.cmd                 Maven Wrapper
│   ├── Dockerfile                      Imagen del backend
│   ├── src/main/java/com/restaurante/ Código Java
│   ├── src/main/resources/
│   │   ├── application.yml             Configuración común
│   │   ├── application-dev.yml         Configuración de desarrollo
│   │   ├── application-prod.yml        Configuración de producción
│   │   └── db/migration/               Migraciones Flyway
│   └── src/test/                       Pruebas
├── database/                           Scripts de backup y restore
├── docs/                               Documentación adicional
├── docker-compose.yml                  PostgreSQL local
├── docker-compose.prod.yml             Despliegue de producción
└── .env.example                        Ejemplo para base externa
```

Puntos de entrada importantes:

- `backend/src/main/java/com/restaurante/RestaurantApplication.java`: inicia Spring Boot.
- `backend/src/main/java/com/restaurante/usuarios/infrastructure/security/SecurityConfig.java`: reglas HTTP, JWT y CORS.
- `backend/src/main/java/com/restaurante/shared/web/GlobalExceptionHandler.java`: formato global de errores.
- `backend/src/main/java/com/restaurante/usuarios/PermisoCodigo.java`: catálogo de permisos.
- `backend/src/main/resources/db/migration/`: definición y evolución de la base de datos.

## 4. Requisitos de desarrollo

Instalar:

1. JDK 25, preferiblemente Eclipse Temurin.
2. Docker Desktop.
3. Git.
4. Opcional: IntelliJ IDEA para editar y depurar Java.
5. Opcional: Postman, Insomnia o Bruno para probar HTTP.

Verificar en PowerShell:

```powershell
java -version
docker version
docker compose version
```

`java -version` debe mostrar Java 25. `docker version` debe mostrar tanto el cliente como el servidor; si no aparece el servidor, inicia Docker Desktop y espera a que termine de arrancar.

## 5. Instalación de dependencias

No se instalan librerías Java manualmente. Maven lee `backend/pom.xml` y descarga todo lo necesario.

En Windows:

```powershell
cd C:\restaurant-management-system\backend
.\mvnw.cmd dependency:resolve
```

En Git Bash, Linux o macOS:

```bash
cd backend
./mvnw dependency:resolve
```

La primera ejecución necesita Internet para descargar las dependencias en el directorio local de Maven (`.m2`).

### Problema observado con `mvnw.cmd`

En el equipo inspeccionado, el wrapper para Windows falló dentro de su sección PowerShell antes de ejecutar Maven. Si ocurre el mensaje `Cannot start maven from wrapper`, las opciones prácticas son:

- ejecutar `./mvnw` desde Git Bash;
- abrir `backend/pom.xml` en IntelliJ y usar el Maven integrado;
- instalar Maven y ejecutar `mvn`;
- regenerar o actualizar Maven Wrapper en un cambio separado.

Este problema pertenece al script de arranque de Maven, no al código Java del sistema.

## 6. Puesta en marcha local

### Paso 1: iniciar PostgreSQL

Desde la raíz del repositorio:

```powershell
cd C:\restaurant-management-system
docker compose up -d postgres
```

Comprobar su estado:

```powershell
docker compose ps
docker compose logs postgres
```

Credenciales del Compose local:

| Valor | Configuración |
|---|---|
| Host | `localhost` |
| Puerto | `5432` |
| Base | `restaurante` |
| Usuario | `restaurante` |
| Contraseña | `restaurante` |

### Paso 2: definir variables locales

La aplicación exige `DB_PASSWORD`. En PowerShell:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "restaurante"
$env:DB_USER = "restaurante"
$env:DB_PASSWORD = "restaurante"
```

No copies sin revisar `.env.example`: actualmente contiene datos de una base externa y no coincide con PostgreSQL local.

En Git Bash:

```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=restaurante
export DB_USER=restaurante
export DB_PASSWORD=restaurante
```

### Paso 3: ejecutar Spring Boot

Con Maven Wrapper:

```powershell
cd C:\restaurant-management-system\backend
.\mvnw.cmd spring-boot:run
```

Con Maven instalado:

```powershell
mvn spring-boot:run
```

Con Git Bash:

```bash
cd backend
./mvnw spring-boot:run
```

También se puede construir el JAR:

```powershell
.\mvnw.cmd clean package -DskipTests
java -jar target\restaurant-management-system-0.0.1-SNAPSHOT.jar
```

La aplicación queda normalmente en `http://localhost:8080`.

### Paso 4: verificar el arranque

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{
  "status": "UP"
}
```

Direcciones útiles:

- Health: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Menú público: `http://localhost:8080/api/v1/menu`

## 7. Usuario inicial

En desarrollo, si todavía no existe, se crea:

```text
usuario: admin
contraseña: admin123
rol: ADMIN
```

El comportamiento está en `usuarios/application/DataInitializer.java`.

Nunca se debe usar `admin123` en producción. Allí la contraseña proviene de `ADMIN_INITIAL_PASSWORD`.

## 8. Autenticación y ejemplo de prueba

### Iniciar sesión desde PowerShell

```powershell
$body = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

$login = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/v1/auth/login" `
    -ContentType "application/json" `
    -Body $body

$token = $login.accessToken
$login
```

Usar el access token:

```powershell
$headers = @{
    Authorization = "Bearer $token"
}

Invoke-RestMethod `
    -Uri "http://localhost:8080/api/v1/mesas" `
    -Headers $headers
```

### Tokens

- El access token dura **1 hora** por defecto (`JWT_EXPIRATION_MS=3600000`).
- El refresh token dura 7 días por defecto.
- El refresh token es de un solo uso y se rota al renovarlo.
- Reutilizar un refresh token antiguo puede revocar toda su familia.
- Cambiar contraseña, rol o estado invalida sesiones anteriores mediante `sesion_version`.

Una parte de la documentación antigua menciona 24 horas para el access token, pero la configuración ejecutable actual usa una hora.

### Límite de login

`POST /api/v1/auth/login` permite cinco intentos por minuto por IP. Si se excede, responde `429 Too Many Requests`. El límite se deshabilita en el perfil de pruebas.

## 9. Arquitectura del código

Los módulos están en:

```text
backend/src/main/java/com/restaurante/
```

Módulos actuales:

```text
anulaciones
auditoria
caja
catalogo
clientes
comandas
configuracion
cuentas
facturacion
finanzas
mesas
pagos
pedidos
reportes
shared
usuarios
```

Cada módulo normalmente contiene:

```text
domain/          Entidades, estados y reglas de negocio
application/     Servicios y casos de uso
web/             Controllers y DTO de la API
infrastructure/  Repositorios, seguridad e integraciones
```

Flujo habitual de una petición:

```text
HTTP
  ↓
Controller
  ↓
DTO de entrada + validación
  ↓
Service / caso de uso
  ↓
Entidad de dominio y reglas
  ↓
Repository / JPA
  ↓
PostgreSQL
```

Una operación también puede publicar eventos o crear registros en el outbox.

### Responsabilidad de cada capa

- **Controller:** recibe HTTP, parámetros y JSON; no debería contener reglas complejas.
- **DTO:** define el contrato JSON y sus validaciones.
- **Service:** coordina el caso de uso y la transacción.
- **Domain:** protege estados e invariantes del negocio.
- **Repository:** consulta y guarda datos.
- **Infrastructure:** implementa detalles técnicos.

## 10. Flujo operativo principal

### Pedido presencial

```text
Configurar catálogo y mesas
        ↓
Crear pedido BORRADOR y ocupar la mesa
        ↓
Agregar, modificar o quitar líneas
        ↓
Confirmar con Idempotency-Key
        ↓
Congelar nombres y precios + crear cuenta
        ↓
Generar una comanda por área de preparación
        ↓
Registrar órdenes de impresión en outbox
        ↓
Cocina: PENDIENTE → EN_PREPARACION → LISTO
        ↓
Solicitar y resolver anulaciones si corresponde
        ↓
Abrir caja
        ↓
Cobrar la cuenta y emitir comprobante
        ↓
Cerrar cuenta y liberar mesa
        ↓
Cerrar caja y conciliar diferencia
```

Estados de pedido:

```text
BORRADOR → CONFIRMADO → EN_PREPARACION → LISTO → ENTREGADO
                                                       ↘ ANULADO
```

Estados de mesa:

```text
LIBRE, OCUPADA, RESERVADA, INACTIVA
```

Estados de comanda:

```text
PENDIENTE → EN_PREPARACION → LISTO
```

### Qué ocurre al confirmar un pedido

1. `PedidoController` recibe `POST /pedidos/{codigo}/confirmar`.
2. Spring Security valida el JWT y el permiso.
3. `PedidoService` carga el pedido.
4. El dominio comprueba que sea un borrador válido y tenga líneas.
5. Se congelan nombres, precios, extras y personalizaciones.
6. El pedido pasa a `CONFIRMADO`.
7. Se crea o actualiza la cuenta de la mesa.
8. Se generan comandas agrupadas por área.
9. Se crean trabajos de impresión en el outbox.
10. La transacción completa se confirma; si algo falla, se revierte.

### Pedido del cliente

1. El cliente autenticado consulta el menú.
2. Crea un pedido con sus líneas y una clave de idempotencia.
3. Confirma método de pago y entrega.
4. Para domicilio debe usar una dirección válida.
5. Se congelan los precios y se generan comandas/outbox.
6. Cocina o una tablet cambia el estado.
7. El cliente consulta por REST o recibe cambios por WebSocket.
8. El pedido queda en su historial.

## 11. API y endpoints

Base URL:

```text
http://localhost:8080/api/v1
```

Salvo login, refresh, menú público, health, Swagger y el handshake WebSocket, las operaciones requieren:

```http
Authorization: Bearer <accessToken>
```

### Autenticación

| Método | Ruta | Función |
|---|---|---|
| POST | `/auth/login` | Iniciar sesión |
| POST | `/auth/refresh` | Rotar refresh token y renovar acceso |
| POST | `/auth/logout` | Revocar refresh token |
| GET | `/auth/me` | Consultar usuario y permisos actuales |

Login:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Refresh/logout:

```json
{
  "refreshToken": "token-recibido-en-login"
}
```

### Usuarios

| Método | Ruta | Función |
|---|---|---|
| POST | `/usuarios` | Crear usuario |
| GET | `/usuarios?pagina=0&tamanio=20` | Lista paginada |
| GET | `/usuarios/{id}` | Obtener usuario |
| PUT | `/usuarios/{id}` | Actualizar nombre, rol o estado |
| PATCH | `/usuarios/{id}/password` | Cambiar contraseña |
| DELETE | `/usuarios/{id}` | Desactivar usuario |
| GET | `/usuarios/roles` | Listar roles y permisos |

Crear usuario:

```json
{
  "username": "mesero1",
  "nombre": "Mesero Uno",
  "password": "clave-segura",
  "rolCodigo": "MESERO"
}
```

Actualizar:

```json
{
  "nombre": "Nombre actualizado",
  "rolCodigo": "CAJERO",
  "activo": true
}
```

Cambiar contraseña:

```json
{
  "nuevaPassword": "otra-clave-segura"
}
```

`DELETE` desactiva lógicamente al usuario; no borra necesariamente su registro físico.

### Catálogo

Categorías:

```text
GET    /categorias
GET    /categorias/{id}
POST   /categorias
PUT    /categorias/{id}
DELETE /categorias/{id}
```

Áreas de preparación:

```text
GET    /areas?soloActivas=false
GET    /areas/{id}
POST   /areas
PUT    /areas/{id}
DELETE /areas/{id}
```

Extras:

```text
GET    /extras
GET    /extras/{id}
POST   /extras
PUT    /extras/{id}
DELETE /extras/{id}
```

Productos:

```text
GET    /productos
GET    /productos/{id}
POST   /productos
PUT    /productos/{id}
DELETE /productos/{id}
```

Menú público, sin autenticación:

```text
GET /menu
```

Los precios de pedidos siempre se obtienen del catálogo en el servidor. Nunca deben confiarse precios enviados por el frontend.

### Mesas

```text
GET    /mesas?soloActivas=true
GET    /mesas/{id}
POST   /mesas
PUT    /mesas/{id}
PATCH  /mesas/{id}/estado
DELETE /mesas/{id}
```

Crear o actualizar:

```json
{
  "numero": 1,
  "capacidad": 4,
  "ubicacion": "Terraza"
}
```

Cambiar estado:

```json
{
  "estado": "RESERVADA"
}
```

### Pedidos presenciales

```text
POST   /pedidos
GET    /pedidos?mesaId={id}
GET    /pedidos/page?page=0&size=20&sort=id
GET    /pedidos/{codigo}
POST   /pedidos/{codigo}/items
PUT    /pedidos/{codigo}/items/{lineaId}
DELETE /pedidos/{codigo}/items/{lineaId}
POST   /pedidos/{codigo}/confirmar
DELETE /pedidos/{codigo}
```

Crear borrador:

```json
{
  "codigo": "PED-2026-0001",
  "mesaId": 1,
  "notas": "Cliente junto a la ventana"
}
```

Agregar producto:

```json
{
  "productoId": 1,
  "cantidad": 2,
  "extraIds": [1, 2],
  "ingredientesRemovidos": ["cebolla"],
  "observaciones": "Sin picante"
}
```

Al confirmar conviene enviar una clave única:

```http
Idempotency-Key: 78d95d86-7744-45eb-8a4a-e280cc7c7e55
```

La idempotencia evita confirmar dos veces si una llamada se repite por un problema de red.

### Comandas e impresión

```text
GET  /comandas?estado=PENDIENTE
GET  /comandas/{id}
POST /comandas/{id}/en-preparacion
POST /comandas/{id}/listo
POST /comandas/{id}/reimprimir
GET  /comandas/impresion/pendientes
POST /comandas/impresion/{id}/enviado
POST /comandas/impresion/{id}/error
```

Error de impresión:

```json
{
  "motivo": "La impresora no responde"
}
```

El backend no imprime directamente. Guarda trabajos en la tabla `outbox`; un agente local debe consultarlos, imprimir y marcarlos como enviados o fallidos.

Estados de outbox:

```text
PENDIENTE, ENVIADO, FALLIDO
```

### Configuración e impresoras

```text
GET /configuracion
GET /configuracion/{clave}
PUT /configuracion/{clave}
```

Actualizar parámetro:

```json
{
  "valor": "Mi Restaurante"
}
```

Impresoras:

```text
POST   /impresoras
GET    /impresoras
GET    /impresoras/{id}
PUT    /impresoras/{id}
DELETE /impresoras/{id}
```

Tipos admitidos:

```text
TERMICA_RED, TERMICA_USB, ESCPOS
```

### Anulaciones

```text
POST  /pedidos/{codigo}/items/{lineaId}/anulaciones
PATCH /anulaciones/{id}/aprobar
PATCH /anulaciones/{id}/rechazar
GET   /anulaciones?pedidoCodigo={codigo}&estado=SOLICITADA
```

Solicitud:

```json
{
  "cantidad": 1,
  "motivo": "El cliente cambió de opinión"
}
```

Estados:

```text
SOLICITADA, APROBADA, RECHAZADA
```

Una aprobación descuenta de la cuenta y genera una comanda de cancelación. Un rechazo conserva la operación para auditoría, pero no descuenta.

### Cuentas

```text
GET   /cuentas?mesaId={id}
GET   /cuentas/page?page=0&size=20
GET   /cuentas/{id}
POST  /cuentas/{id}/adiciones
PATCH /cuentas/{id}/cerrar
```

Crear una adición en la misma cuenta:

```json
{
  "codigo": "PED-2026-0002",
  "notas": "Segunda ronda"
}
```

### Pagos

```text
POST /cuentas/{cuentaId}/cobros
GET  /pagos?cuentaId={id}
GET  /pagos/{id}
```

Pago mixto:

```json
{
  "pagos": [
    {
      "metodo": "EFECTIVO",
      "monto": 10.00
    },
    {
      "metodo": "TARJETA",
      "monto": 15.50
    }
  ],
  "documento": {
    "tipo": "FACTURA",
    "clienteNombre": "Ana Pérez",
    "clienteIdentificacion": "1234567890"
  }
}
```

Métodos disponibles:

```text
EFECTIVO, TARJETA, TRANSFERENCIA, OTRO
```

La suma de pagos debe coincidir exactamente con el total. Debe existir una caja abierta. Al cobrar se registran los pagos, se emite el comprobante, se cierra la cuenta y se libera la mesa.

### Caja

```text
GET  /cajas
GET  /cajas/abierta
GET  /cajas/{id}
GET  /cajas/{id}/movimientos
POST /cajas/apertura
POST /cajas/{id}/egresos
POST /cajas/{id}/cierre
```

Apertura:

```json
{
  "montoInicial": 100.00
}
```

Egreso:

```json
{
  "concepto": "Compra urgente de hielo",
  "monto": 8.50,
  "metodo": "EFECTIVO"
}
```

Cierre:

```json
{
  "montoReal": 241.75
}
```

Solo puede existir una caja abierta al mismo tiempo. El cierre calcula la diferencia entre el monto esperado y el real.

### Comprobantes

```text
GET /comprobantes?cuentaId={id}
GET /comprobantes/{id}
```

La emisión ocurre normalmente dentro del caso de uso de cobro.

### Clientes

```text
GET  /clientes/carrito
GET  /clientes/menu
POST /clientes/pedidos
POST /clientes/pedidos/{codigo}/confirmar
POST /clientes/pedidos/{codigo}/en-preparacion
POST /clientes/pedidos/{codigo}/listo
GET  /clientes/pedidos/{codigo}
GET  /clientes/historial?page=0&size=20
POST /clientes/direcciones
```

Estos endpoints requieren usuario y permisos de cliente. No deben confundirse con `GET /api/v1/menu`, que es público.

Dirección:

```json
{
  "etiqueta": "Casa",
  "direccion": "Av. Principal 123",
  "telefono": "0999999999",
  "observaciones": "Casa de puerta azul"
}
```

Estados de pedido del cliente:

```text
BORRADOR, CONFIRMADO, EN_PREPARACION, LISTO, ENTREGADO, ANULADO
```

### Finanzas, reportes y auditoría

```text
GET /finanzas/resumen?desde=...&hasta=...
GET /reportes/ventas?desde=...&hasta=...
GET /auditoria?desde=...&hasta=...&entidad=...
```

Las fechas son ISO-8601:

```text
2026-09-01T00:00:00Z
2026-09-30T23:59:59Z
```

### Operación y documentación

```text
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
GET /v3/api-docs
GET /swagger-ui.html
```

Swagger y OpenAPI están desactivados en el perfil de producción.

## 12. WebSocket/STOMP

Handshake:

```text
/ws
```

Suscripción:

```text
/topic/pedido/{codigo}
```

El JWT se envía en la conexión STOMP. El backend comprueba que el usuario pueda ver ese pedido; otro cliente no debería poder suscribirse al pedido ajeno.

Se usa para comunicar cambios como:

```text
CONFIRMADO → EN_PREPARACION → LISTO
```

## 13. Roles y permisos

Roles incluidos:

- `ADMIN`: todos los permisos.
- `MESERO`: catálogo y mesas en lectura, pedidos, comandas, cuentas y solicitudes de anulación.
- `COCINA`: catálogo, comandas y cambios de preparación.
- `CAJERO`: cobros, caja, cuentas, facturación, finanzas y reportes.
- `CLIENTE`: menú, pedido propio, historial, dirección y estado en tiempo real.

La seguridad se basa en permisos específicos, no solamente en el nombre del rol. Algunos ejemplos:

```text
usuarios:ver
usuarios:crear
catalogo:gestionar
mesas:gestionar
pedidos:crear
pedidos:editar
pedidos:confirmar
comandas:ver
anulaciones:aprobar
cuentas:gestionar
pagos:cobrar
caja:apertura
caja:cierre
reportes:ver
auditoria:ver
```

Los controllers declaran los permisos mediante `@PreAuthorize`. Los códigos están centralizados en `PermisoCodigo.java` y las asignaciones iniciales están en las migraciones `V1`, `V6` y `V9`.

## 14. Base de datos y Flyway

Migraciones actuales:

| Migración | Contenido principal |
|---|---|
| `V1` | Usuarios, roles, permisos y refresh tokens |
| `V2` | Catálogo, mesas, parámetros e impresoras |
| `V3` | Pedidos y líneas |
| `V4` | Comandas y outbox |
| `V5` | Anulaciones y cuentas |
| `V6` | Permiso adicional para cajero |
| `V7` | Pagos, comprobantes y caja |
| `V8` | Auditoría |
| `V9` | Clientes y pedidos de clientes |
| `V10` | Versión de sesión para revocación |

### Regla crítica

Nunca edites una migración que ya se ejecutó en una base compartida o de producción. Flyway conserva su checksum y detectaría la modificación.

Para cambiar el esquema, crea una nueva migración:

```text
backend/src/main/resources/db/migration/V11__descripcion_del_cambio.sql
```

Ejemplo:

```sql
ALTER TABLE productos
ADD COLUMN codigo_barras VARCHAR(50);

CREATE UNIQUE INDEX uq_productos_codigo_barras
ON productos (codigo_barras)
WHERE codigo_barras IS NOT NULL;
```

Hibernate utiliza:

```yaml
ddl-auto: validate
```

Eso significa que Hibernate no modifica tablas: Flyway crea el esquema y Hibernate solo comprueba que las entidades coincidan.

## 15. Pruebas automatizadas

### Ejecutar toda la suite

Docker Desktop debe estar funcionando porque las integraciones usan Testcontainers:

```powershell
cd C:\restaurant-management-system\backend
.\mvnw.cmd test
```

Testcontainers inicia un PostgreSQL aislado. No debería borrar ni usar la base de desarrollo.

### Ejecutar una clase

```powershell
.\mvnw.cmd test -Dtest=PedidoTest
```

### Ejecutar un método

```powershell
.\mvnw.cmd test -Dtest=PedidoTest#confirmarBloqueaElBorrador
```

### Compilación y verificación completa

```powershell
.\mvnw.cmd clean verify
```

### Prueba de carga

```powershell
.\mvnw.cmd test -Dtest=CargaConcurrenteTest
```

La prueba de carga utiliza 50 hilos y verifica idempotencia y ausencia de duplicados.

### Cobertura existente

Hay pruebas para:

- dinero y redondeo;
- entidades y transiciones de estado;
- login, refresh, logout y permisos;
- rotación y revocación de sesión;
- catálogo y mesas;
- pedidos y comandas;
- cuentas y anulaciones;
- caja, cobros y facturación;
- auditoría, finanzas y reportes;
- pedidos de clientes;
- WebSocket y aislamiento entre clientes;
- concurrencia e idempotencia;
- límites de módulos con Spring Modulith;
- carga concurrente.

Durante el análisis no se pudo ejecutar nuevamente la suite completa porque Docker Desktop estaba detenido y el entorno de Maven no pudo descargar el parent POM por restricción de red. Existen clases compiladas previamente en `backend/target`, pero eso no reemplaza una ejecución nueva.

## 16. Pruebas manuales recomendadas

Una prueba completa mínima debería seguir este orden:

1. Consultar `/actuator/health`.
2. Iniciar sesión como `admin`.
3. Crear área, categoría, extra y producto.
4. Crear una mesa.
5. Crear un pedido en borrador.
6. Agregar una línea.
7. Confirmar con `Idempotency-Key`.
8. Consultar la cuenta y las comandas creadas.
9. Marcar la comanda en preparación y lista.
10. Abrir una caja.
11. Cobrar exactamente el total de la cuenta.
12. Consultar comprobante y movimientos de caja.
13. Cerrar la caja.
14. Consultar reportes y auditoría.

Swagger es la opción más sencilla para descubrir los esquemas exactos de cada request y ejecutar esta secuencia.

## 17. Formato de errores

La API utiliza `ProblemDetail`, siguiendo RFC 7807. Un error puede ser:

```json
{
  "type": "urn:problem:restaurante:business-rule",
  "title": "Regla de negocio",
  "status": 422,
  "detail": "No se puede confirmar un pedido sin líneas",
  "path": "/api/v1/pedidos/PED-1/confirmar"
}
```

Códigos importantes:

| Código | Significado habitual |
|---|---|
| `400` | JSON o validación incorrecta |
| `401` | Token ausente, vencido o inválido |
| `403` | Usuario autenticado sin permiso |
| `404` | Recurso inexistente |
| `409` | Duplicado, concurrencia o conflicto de estado |
| `422` | Regla de negocio incumplida |
| `429` | Demasiados intentos de login |
| `500` | Error inesperado |

Los errores de validación pueden incluir `fieldErrors` con los campos incorrectos.

## 18. Cómo modificar el sistema sin conocer Java

Para un cambio normal:

1. Localiza el endpoint en un archivo `*Controller.java`.
2. Identifica el método del `*Service` invocado.
3. Revisa la entidad o regla correspondiente en `domain`.
4. Revisa el repositorio en `infrastructure`.
5. Si cambia la base, crea una migración nueva.
6. Modifica o crea DTO de entrada y salida.
7. Agrega una prueba que falle antes del cambio.
8. Implementa el cambio.
9. Ejecuta primero la prueba específica.
10. Ejecuta toda la suite.
11. Prueba manualmente desde Swagger.

Ejemplo: agregar código de barras a un producto probablemente involucra:

```text
catalogo/domain/Producto.java
catalogo/web/dto/ProductoRequest.java
catalogo/web/dto/ProductoResponse.java
catalogo/application/ProductoService.java
db/migration/V11__producto_codigo_barras.sql
catalogo/CatalogoIntegrationTest.java
```

### Conceptos básicos de Java/Spring

| Concepto | Significado |
|---|---|
| `class` | Tipo con datos y comportamiento |
| `record` | Estructura inmutable, frecuente en JSON de entrada/salida |
| `interface` | Contrato que debe implementar otra pieza |
| `enum` | Lista fija de valores permitidos |
| `@RestController` | Clase que expone endpoints HTTP |
| `@Service` | Implementa casos de uso |
| `@Entity` | Clase persistida en una tabla |
| `@Repository` | Acceso a la base de datos |
| `@Transactional` | Confirma o revierte una operación completa |
| `@Valid` | Activa validaciones del JSON |
| `@PreAuthorize` | Exige permisos antes de ejecutar |
| `Optional<T>` | Resultado que podría no existir |
| `List<T>` | Lista de objetos |
| `BigDecimal` | Decimal preciso, apropiado para dinero |

### Reglas para no romper el diseño

- No uses `double` o `float` para dinero.
- No aceptes precios calculados por el frontend.
- No edites migraciones ya aplicadas.
- No pongas lógica de negocio compleja en controllers.
- Mantén las operaciones de varios pasos dentro de transacciones.
- Conserva la idempotencia de confirmaciones y cobros.
- No devuelvas entidades JPA directamente; usa DTO.
- No subas `.env`, contraseñas o secretos JWT a Git.
- No elimines `@Version` de entidades protegidas contra concurrencia.
- Agrega `@PreAuthorize` a endpoints protegidos.
- Refuerza reglas críticas con constraints e índices de PostgreSQL.
- Ejecuta `ModularityTests` al agregar dependencias entre módulos.

## 19. Configuración

Variables principales:

| Variable | Desarrollo/default | Uso |
|---|---|---|
| `DB_HOST` | `localhost` | Host PostgreSQL |
| `DB_PORT` | `5432` | Puerto PostgreSQL |
| `DB_NAME` | `restaurante` | Nombre de base |
| `DB_USER` | `restaurante` | Usuario de base |
| `DB_PASSWORD` | Obligatoria | Contraseña de base |
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `JWT_SECRET` | Secreto dev incorporado | Firma JWT |
| `JWT_EXPIRATION_MS` | `3600000` | Duración access token |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` | Duración refresh token |
| `IVA_RATE` | `0.15` | IVA incluido en precios |
| `ADMIN_INITIAL_PASSWORD` | `admin123` en dev | Contraseña inicial |
| `CORS_ALLOWED_ORIGINS` | `*` en dev | Orígenes del navegador |

En desarrollo se muestran consultas SQL y logs `DEBUG`, por lo que la consola puede ser extensa.

## 20. Producción

Para producción, define como mínimo:

```text
SPRING_PROFILES_ACTIVE=prod
DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD
JWT_SECRET
ADMIN_INITIAL_PASSWORD
CORS_ALLOWED_ORIGINS
```

Ejemplo conceptual:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_HOST = "servidor-postgres"
$env:DB_PORT = "5432"
$env:DB_NAME = "restaurante"
$env:DB_USER = "usuario_seguro"
$env:DB_PASSWORD = "contraseña-segura"
$env:JWT_SECRET = "secreto-aleatorio-de-al-menos-32-caracteres"
$env:ADMIN_INITIAL_PASSWORD = "contraseña-admin-segura"
$env:CORS_ALLOWED_ORIGINS = "https://app.midominio.com"
```

Consideraciones:

- `JWT_SECRET` debe tener al menos 32 caracteres; es mejor generar uno aleatorio más largo.
- No uses `*` para CORS en producción.
- Swagger/OpenAPI se desactiva en `prod`.
- El backend debe desplegarse detrás de HTTPS.
- El proxy debe reenviar correctamente WebSocket (`Upgrade` y `Connection`).
- Los secretos deben proceder del entorno o de un gestor de secretos.
- PostgreSQL debe tener backups periódicos probados.
- Cambiar `JWT_SECRET` invalida todos los tokens firmados con el anterior.

La documentación ampliada está en `docs/produccion.md`.

## 21. Docker

### Solo base local

```powershell
docker compose up -d postgres
```

### Detener contenedores sin borrar datos

```powershell
docker compose down
```

No agregues `-v` salvo que quieras eliminar también el volumen y todos los datos de PostgreSQL.

### Construir la imagen del backend

```powershell
cd backend
docker build -t restaurant-backend .
```

El `Dockerfile` hace un build multi-stage con JDK 25 y ejecuta el resultado con JRE 25 como usuario no root.

## 22. Copias de seguridad

El repositorio incluye scripts en `database/` y `scripts/`. Antes de ejecutar un restore, verifica cuidadosamente la base objetivo porque restaurar puede reemplazar información.

La estrategia documentada usa `pg_dump` en formato custom y valida con `pg_restore --list`.

Consultar:

```text
database/README.MD
database/backup.sh
database/restore.sh
```

## 23. Aspectos destacados y observaciones

1. El perfil `dev` se activa por defecto; producción debe activar `prod` explícitamente.
2. El backend local no inicia sin `DB_PASSWORD`.
3. `.env.example` no corresponde a las credenciales del Compose local.
4. El access token efectivo dura una hora, aunque una documentación antigua diga 24 horas.
5. CORS permite cualquier origen en desarrollo, pero producción exige una lista.
6. Los refresh tokens se almacenan como hash y se rotan.
7. Cambios sensibles de usuario invalidan sesiones anteriores.
8. El handshake `/ws` es público, pero el canal STOMP valida JWT y propiedad del pedido.
9. La impresión está desacoplada mediante outbox.
10. Hay restricciones únicas e idempotencia para proteger frente a concurrencia.
11. La base tiene índices parciales para garantizar una cuenta abierta por mesa y una sola caja abierta.
12. El dinero tiene escala de dos decimales y redondeo `HALF_UP`.
13. `open-in-view` está deshabilitado, lo cual obliga a cargar datos dentro de servicios/transacciones.
14. `ddl-auto: validate` evita modificaciones accidentales de esquema por Hibernate.

## 24. Flujo de trabajo recomendado para desarrollar

```text
1. Actualizar la rama local
2. Iniciar Docker/PostgreSQL
3. Crear una rama para el cambio
4. Entender controller → service → domain → repository
5. Escribir o modificar una prueba
6. Implementar el cambio
7. Ejecutar la prueba específica
8. Ejecutar clean verify
9. Probar el endpoint en Swagger
10. Revisar git diff y no incluir secretos/target
11. Crear commit y pull request
```

Comandos habituales:

```powershell
git status
git diff
.\mvnw.cmd test -Dtest=NombreDelTest
.\mvnw.cmd clean verify
```

El directorio `target/` es generado por Maven y no debería editarse ni agregarse al control de versiones.

## 25. Diagnóstico rápido

### La aplicación no conecta a PostgreSQL

Revisar:

```powershell
docker compose ps
docker compose logs postgres
$env:DB_PASSWORD
```

Comprueba que el puerto `5432` no esté ocupado por otro PostgreSQL.

### Responde 401

- Falta `Authorization: Bearer ...`.
- El token venció.
- Se cambió el usuario, contraseña o rol.
- Se está enviando el refresh token como access token.

### Responde 403

El token es válido, pero el usuario no tiene el permiso requerido.

### Flyway falla con checksum

Probablemente se modificó una migración ya aplicada. Restaura el archivo original y crea una migración nueva.

### Testcontainers no inicia

- Inicia Docker Desktop.
- Comprueba `docker version`.
- Revisa que el usuario pueda acceder al motor de Docker.

### Maven no descarga dependencias

- Comprueba Internet, proxy y firewall.
- Prueba desde IntelliJ o con Maven instalado.
- No uses modo offline hasta que todas las dependencias estén descargadas.

### El puerto 8080 está ocupado

```powershell
$env:SERVER_PORT = "8081"
.\mvnw.cmd spring-boot:run
```

La API quedará en `http://localhost:8081`.

## 26. Lista de comprobación antes de entregar un cambio

- [ ] El proyecto compila.
- [ ] La prueba específica pasa.
- [ ] Toda la suite pasa con Docker.
- [ ] El endpoint fue probado manualmente.
- [ ] No se editó una migración anterior.
- [ ] Se agregó una migración nueva si cambió el esquema.
- [ ] No se usó `double` para dinero.
- [ ] El endpoint tiene autorización adecuada.
- [ ] Los request usan validaciones.
- [ ] No hay secretos ni `.env` en Git.
- [ ] No se agregó `target/`.
- [ ] Se revisó `git diff`.
- [ ] Swagger o la documentación se actualizaron si cambió el contrato.

---

La forma más segura de empezar es levantar PostgreSQL, iniciar el backend en perfil `dev`, entrar a Swagger con `admin/admin123` y recorrer el flujo de mesa → pedido → comanda → caja → cobro. Para cualquier modificación, seguir el recorrido controller → service → dominio → repositorio → migración → prueba permite entender el impacto antes de tocar el código.
