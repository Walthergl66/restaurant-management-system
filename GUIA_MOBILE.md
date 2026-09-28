# Guía completa de la aplicación móvil

Esta guía explica cómo está construida, cómo se instala, cómo se ejecuta, cómo se conecta con el backend y qué partes faltan para completar la aplicación móvil de `restaurant-management-system`.

> La fuente de verdad de esta guía es el código actual de `mobile/`. La aplicación puede mostrar el menú sin backend porque actualmente usa datos de demostración. Esto no significa que toda la integración esté terminada.

## 1. Resumen del proyecto

`mobile/` es una aplicación de cliente para **Estación Burger**, desarrollada con React Native y Expo. Su objetivo es permitir que un cliente:

- consulte el menú y filtre productos por categoría;
- vea el detalle de un producto;
- seleccione extras y cantidades;
- administre un carrito;
- inicie sesión;
- consulte pedidos y su perfil;
- cree y confirme pedidos contra el backend.

La interfaz usa un tema oscuro con colores neón. La navegación principal tiene cuatro pestañas: Menú, Carrito, Pedidos y Perfil.

En el estado actual, la aplicación es una base visual funcional, pero no un flujo comercial completo. El menú y los pedidos visibles usan datos de demostración, y el botón de pago todavía no crea pedidos.

## 2. Tecnologías utilizadas

| Tecnología | Uso |
|---|---|
| React 19 | Componentes y estado de la interfaz |
| React Native 0.86 | Aplicación móvil multiplataforma |
| Expo 57 | Entorno de desarrollo, compilación y ejecución |
| Expo Router | Navegación basada en archivos |
| TypeScript 6 | Tipado estático |
| AsyncStorage | Persistencia local de sesión y tokens |
| Expo Linear Gradient | Fondos y botones con degradados |
| Ionicons | Iconografía |
| Fetch API | Peticiones HTTP al backend |

El proyecto usa `npm` y el archivo `package-lock.json` fija las versiones instaladas.

## 3. Estructura del proyecto

```text
mobile/
├── app.json                         Configuración de Expo, Android e iOS
├── package.json                     Dependencias y comandos npm
├── package-lock.json                Versiones exactas de dependencias
├── tsconfig.json                    TypeScript estricto y alias @/*
├── assets/                          Iconos, splash, favicon y logotipo
└── src/
    ├── app/                         Pantallas y navegación Expo Router
    │   ├── _layout.tsx              Proveedores globales y Stack raíz
    │   ├── login.tsx                Inicio de sesión
    │   ├── product/[id].tsx         Detalle dinámico de producto
    │   └── (tabs)/
    │       ├── _layout.tsx          Barra inferior de navegación
    │       ├── index.tsx            Menú principal
    │       ├── cart.tsx             Carrito
    │       ├── orders.tsx           Pedidos
    │       └── profile.tsx          Perfil
    ├── components/                  Componentes reutilizables
    │   └── ui/                      Button, Input, Card y Badge
    ├── services/
    │   ├── api.ts                   Cliente HTTP y token Bearer
    │   ├── auth.ts                  Login, refresh, logout y usuario actual
    │   ├── menu.ts                  Catálogo real o datos demo
    │   └── pedidos.ts               Creación, confirmación e historial
    ├── store/
    │   ├── AuthContext.tsx          Estado global de autenticación
    │   └── CartContext.tsx          Estado global del carrito
    ├── theme/                       Colores y tipografía
    └── types/index.ts               Contratos TypeScript
```

## 4. Cómo funciona la navegación

Expo Router convierte archivos en rutas:

| Archivo | Ruta conceptual |
|---|---|
| `src/app/(tabs)/index.tsx` | Menú principal |
| `src/app/(tabs)/cart.tsx` | Carrito |
| `src/app/(tabs)/orders.tsx` | Pedidos |
| `src/app/(tabs)/profile.tsx` | Perfil |
| `src/app/login.tsx` | Login |
| `src/app/product/[id].tsx` | Detalle de un producto por ID |

El grupo `(tabs)` no forma parte visible de la URL. El archivo raíz `_layout.tsx` envuelve toda la aplicación con `AuthProvider` y `CartProvider`.

No existe todavía una protección global de rutas. Es posible entrar a las pestañas sin iniciar sesión; el backend rechazará con `401` o `403` las operaciones protegidas.

## 5. Requisitos de desarrollo

Instalar:

1. Node.js compatible con Expo 57. Se recomienda una versión LTS soportada por Expo.
2. npm, incluido con Node.js.
3. Expo Go en un teléfono, o Android Studio con un emulador.
4. El backend de este repositorio ejecutándose en el puerto `8080`.

Verificar desde PowerShell:

```powershell
node --version
npm --version
```

En el equipo analizado se detectaron Node `24.14.1` y npm `11.11.0`. Si Expo presenta incompatibilidades, conviene usar la versión LTS recomendada por la documentación de la versión de Expo instalada.

## 6. Instalar dependencias

Desde la raíz del repositorio:

```powershell
cd C:\restaurant-management-system\mobile
npm ci
```

`npm ci` instala exactamente lo definido en `package-lock.json`. Para agregar una dependencia nueva se utiliza `npm install nombre-paquete`, pero no debe usarse solo para una instalación reproducible.

La carpeta `node_modules/` está ignorada por Git y no debe confirmarse.

## 7. Iniciar la aplicación

### Servidor de desarrollo de Expo

```powershell
cd C:\restaurant-management-system\mobile
npm start
```

El terminal mostrará un código QR y opciones para abrir la aplicación.

### Teléfono físico con Expo Go

1. Conecta el teléfono y la computadora a la misma red Wi-Fi.
2. Instala Expo Go en el teléfono.
3. Ejecuta `npm start`.
4. Escanea el QR con Expo Go o con la cámara del teléfono.

Si la red local bloquea la detección, prueba:

```powershell
npx expo start --tunnel
```

El túnel de Expo permite cargar el paquete JavaScript, pero no vuelve accesible automáticamente al backend local. La API todavía debe ser alcanzable desde el teléfono.

### Emulador Android

Con Android Studio y un emulador iniciado:

```powershell
npm run android
```

También se puede presionar `a` en el terminal de Expo.

### Navegador web

```powershell
npm run web
```

La versión web es útil para revisar rápidamente la interfaz, pero no sustituye las pruebas en Android/iOS.

## 8. Iniciar y comprobar el backend

En otra terminal:

```powershell
cd C:\restaurant-management-system\backend
.\mvnw.cmd spring-boot:run
```

Comprobar el estado:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

El resultado esperado es `UP`. Swagger está disponible en:

```text
http://localhost:8080/swagger-ui.html
```

## 9. Configurar la URL del backend

Actualmente la URL está escrita directamente en `src/services/api.ts`:

```ts
const BASE_URL = 'http://192.168.2.100:8080/api/v1';
```

Debe coincidir con el lugar desde el cual se ejecuta la app:

| Entorno de la app | URL usual |
|---|---|
| Navegador en la misma computadora | `http://localhost:8080/api/v1` |
| Emulador Android estándar | `http://10.0.2.2:8080/api/v1` |
| Teléfono físico en la misma Wi-Fi | `http://IP_DE_LA_COMPUTADORA:8080/api/v1` |
| Simulador iOS en macOS | `http://localhost:8080/api/v1` |

En la computadora inspeccionada se observó anteriormente la dirección Wi-Fi `192.168.0.115`. Si continúa siendo la IP actual y el teléfono está en la misma red, la URL sería:

```ts
const BASE_URL = 'http://192.168.0.115:8080/api/v1';
```

Verifica la IP actual con:

```powershell
ipconfig
```

Usa la dirección IPv4 del adaptador Wi-Fi activo. No uses `127.0.0.1` desde un teléfono: allí significa “el propio teléfono”. La IP y puerto de PostgreSQL (`127.0.0.1:5555`) tampoco se colocan en la app; el móvil se conecta al backend, y el backend se conecta a PostgreSQL.

Prueba desde el navegador del teléfono:

```text
http://IP_DE_LA_COMPUTADORA:8080/actuator/health
```

Si no abre:

- confirma que backend y teléfono estén en la misma red;
- comprueba que el backend siga ejecutándose;
- permite Java o el puerto `8080` en el Firewall de Windows;
- comprueba que la red no tenga aislamiento de clientes;
- prueba `Test-NetConnection IP_DE_LA_COMPUTADORA -Port 8080` desde otro equipo.

### Recomendación para variables de entorno

La URL no debería permanecer fija en el código. Expo admite variables públicas con prefijo `EXPO_PUBLIC_`. Una evolución recomendada es usar:

```env
EXPO_PUBLIC_API_URL=http://192.168.0.115:8080/api/v1
```

y en `api.ts`:

```ts
const BASE_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080/api/v1';
```

Las variables `EXPO_PUBLIC_` se incluyen en la aplicación compilada. Nunca deben contener contraseñas, secretos JWT ni credenciales de PostgreSQL.

## 10. Modo demostración y modo real

`src/services/menu.ts` contiene:

```ts
const DEMO_MODE = true;
```

Con este valor:

- el menú usa productos, categorías, extras e imágenes definidos en el archivo;
- la pantalla puede funcionar aunque el backend esté apagado;
- cambiar la URL de la API no modifica el menú visible;
- no se comprueba el contrato real del catálogo.

Para empezar a integrar el backend habría que cambiarlo a `false`, pero no basta con hacerlo: existe una diferencia de contrato que se explica en la sección de hallazgos.

La pantalla `orders.tsx` también usa `demoPedidos` y no invoca `pedidosService.getHistorial()`.

## 11. Autenticación

El flujo actual es:

1. `login.tsx` recibe usuario y contraseña.
2. `authService.login()` hace `POST /api/v1/auth/login`.
3. Guarda `accessToken`, `refreshToken` y `usuario` en AsyncStorage.
4. Configura el token Bearer en el cliente HTTP.
5. Redirige a las pestañas.

En desarrollo, el backend crea por defecto:

```text
Usuario: admin
Contraseña: admin123
```

Para probar el login, abre la pantalla Perfil y pulsa “Iniciar sesión”, o navega directamente a `/login` desde el entorno de desarrollo.

Claves almacenadas localmente:

```text
estacion_token
estacion_refresh
estacion_user
```

### Limitaciones actuales de sesión

- Cargar la sesión guardada no valida inmediatamente el token con `/auth/me`.
- El cliente HTTP no intenta renovar automáticamente un access token vencido.
- `refreshSession()` existe, pero no está conectado a una respuesta `401` global.
- AsyncStorage no es almacenamiento cifrado; para producción conviene evaluar `expo-secure-store` para tokens.
- No hay guard de navegación que obligue a autenticarse.

## 12. Cliente HTTP y manejo de errores

`src/services/api.ts` centraliza `GET`, `POST`, `PUT`, `PATCH` y `DELETE`. Cuando hay una sesión, agrega:

```http
Authorization: Bearer <accessToken>
```

Para errores intenta leer JSON y usa `detail` como mensaje. El backend devuelve problemas HTTP compatibles con este enfoque.

Actualmente no implementa:

- timeout o cancelación;
- reintentos;
- renovación automática de token;
- tratamiento específico de falta de red;
- trazas de desarrollo;
- selección de URL por entorno.

## 13. Carrito y cálculo de precios

`CartContext` mantiene el carrito solo en memoria. Al cerrar o recargar la aplicación se pierde.

Dos líneas se consideran iguales cuando tienen el mismo producto y la serialización JSON de los extras es idéntica. El total se calcula sumando precio base y extras por cantidad.

Aspectos a mejorar:

- ordenar los IDs de extras antes de construir la identidad de una línea;
- persistir el carrito o sincronizarlo con `/api/v1/clientes/carrito`;
- evitar `number` para cálculos monetarios sensibles; el backend usa decimal exacto;
- validar disponibilidad y precios nuevamente en el backend;
- agregar observaciones y selección de ingredientes removidos;
- implementar checkout.

El backend siempre debe ser la autoridad final del precio.

## 14. Pedidos

`pedidosService` ya declara operaciones para:

| Operación | Endpoint |
|---|---|
| Crear borrador | `POST /api/v1/clientes/pedidos` |
| Confirmar | `POST /api/v1/clientes/pedidos/{codigo}/confirmar` |
| Consultar pedido | `GET /api/v1/clientes/pedidos/{codigo}` |
| Historial | `GET /api/v1/clientes/historial` |
| Agregar dirección | `POST /api/v1/clientes/direcciones` |

La clave `idempotencyKey` evita crear o confirmar dos veces el mismo pedido ante reintentos. Debe generarse una vez por operación lógica y reutilizarse al repetir exactamente la misma solicitud.

El botón “Proceder al pago” en `cart.tsx` contiene un `TODO`; aún no navega a checkout ni usa este servicio. La pantalla Pedidos usa una lista local y su refresco solo espera un segundo.

## 15. Tema y componentes

Los tokens visuales están centralizados en:

- `src/theme/colors.ts`;
- `src/theme/typography.ts`.

Los componentes reutilizables incluyen:

- `Button`, `Input`, `Card` y `Badge`;
- `Header`;
- `ProductCard`;
- `CartItemRow`;
- `OrderCard`;
- `CategoryFilter`.

Para mantener consistencia, los colores y estilos compartidos deben agregarse al tema en vez de repetirse en cada pantalla.

## 16. Comandos de validación

Después de instalar dependencias:

```powershell
cd C:\restaurant-management-system\mobile
npx tsc --noEmit
npx expo-doctor
```

- `tsc --noEmit` comprueba tipos sin generar archivos.
- `expo-doctor` comprueba compatibilidad de dependencias y configuración Expo.

No hay scripts de lint ni pruebas automatizadas en `package.json`. Tampoco hay archivos de pruebas. Antes de producción se recomienda agregar:

- ESLint;
- pruebas unitarias de servicios y contextos;
- React Native Testing Library para pantallas;
- pruebas E2E, por ejemplo con Maestro o Detox;
- un pipeline CI que ejecute tipos, lint y pruebas.

## 17. Lista de prueba manual

### Arranque

- `npm ci` termina sin errores.
- `npx expo-doctor` no reporta incompatibilidades críticas.
- Expo muestra el QR.
- La app abre sin pantalla roja.

### Interfaz demo

- El menú muestra categorías y productos.
- Cambiar de categoría filtra los productos.
- Abrir un producto muestra descripción, precio y extras.
- Agregar un producto aumenta el indicador del carrito.
- Cambiar cantidades recalcula el total.
- Limpiar elimina todas las líneas.
- Las cuatro pestañas funcionan.

### Integración real

- `/actuator/health` abre desde el teléfono.
- Login con `admin/admin123` devuelve sesión.
- Perfil muestra el usuario autenticado.
- Cerrar sesión borra el usuario local.
- Un endpoint protegido incluye el token Bearer.
- Un token vencido se trata sin dejar la app en un estado incoherente.

### Pedidos, cuando se implemente checkout

- El pedido se crea una sola vez incluso al repetir la petición.
- El total mostrado coincide con el calculado por el backend.
- Confirmar cambia el estado.
- Historial muestra datos reales y paginación.
- El carrito se vacía solamente después de una confirmación exitosa.

## 18. Hallazgos del análisis actual

### 18.1 URL fija y desactualizada

`api.ts` usa `192.168.2.100`, mientras que la red observada anteriormente era `192.168.0.x`. En un teléfono físico, el login probablemente fallará hasta configurar una IP alcanzable.

### 18.2 El menú real no coincide con el tipo esperado

La app espera en `getMenu()`:

```ts
{ categorias: Categoria[]; productos: Producto[] }
```

pero llama `GET /api/v1/menu`. El backend administrativo expone un DTO de menú, mientras que `GET /api/v1/clientes/menu` devuelve una lista simplificada de productos. Antes de desactivar demo debe elegirse el endpoint correcto y adaptar el mapeo.

Además, `ProductoParaPedido` del backend no contiene actualmente descripción, imagen, categoría ni extras, datos que la pantalla espera para su diseño completo.

### 18.3 Tipo incorrecto para ingredientes removidos

La app declara:

```ts
ingredientesRemovidos?: number[];
```

El backend recibe una lista de textos (`List<String>`). El contrato TypeScript debería ser:

```ts
ingredientesRemovidos?: string[];
```

### 18.4 Pedidos todavía simulados

La pantalla Pedidos no llama al backend. `pedidosService` existe, pero aún no está conectado a la UI.

### 18.5 Checkout sin implementar

El botón principal del carrito no realiza ninguna acción. Faltan método de entrega, método de pago, dirección, generación de código, idempotencia, creación y confirmación.

### 18.6 Acciones visuales sin comportamiento

Actualmente no hacen nada o están incompletas:

- “Pedir” en la promoción;
- “Ver Todo”;
- “Olvidé mi contraseña”;
- “Crear cuenta”;
- filas de configuración del perfil;
- selección de una tarjeta de pedido;
- pago desde el carrito.

### 18.7 Dinero convertido a `number`

La API representa precios decimales, pero el carrito usa `parseFloat` y `number`. Puede ser suficiente para una maqueta, pero para importes de producción conviene trabajar en centavos enteros o usar una librería decimal y aceptar siempre el total definitivo del backend.

### 18.8 Sin pruebas automáticas

El proyecto no contiene una suite de pruebas ni comandos `test`/`lint`. La validación disponible inicialmente es TypeScript, Expo Doctor y prueba manual.

## 19. Orden recomendado para completar la aplicación

1. Mover `BASE_URL` a `EXPO_PUBLIC_API_URL`.
2. Corregir el tipo `ingredientesRemovidos`.
3. Definir con el backend un contrato único para el menú móvil.
4. Desactivar `DEMO_MODE` y probar menú real.
5. Agregar guard de autenticación y renovación automática de token.
6. Implementar checkout y conectar `pedidosService`.
7. Sustituir `demoPedidos` por historial real.
8. Implementar detalle y actualización en tiempo real de pedidos.
9. Persistir o sincronizar el carrito.
10. Agregar manejo de estados vacíos, red y reintentos.
11. Añadir lint, pruebas y CI.
12. Configurar builds de desarrollo y producción con EAS.

## 20. Solución de problemas frecuentes

### La app abre, pero siempre muestra productos demo

Es el comportamiento actual porque `DEMO_MODE` vale `true`.

### `Network request failed`

- revisa `BASE_URL`;
- no uses `localhost` desde un teléfono físico;
- abre `/actuator/health` desde el teléfono;
- verifica el Firewall de Windows;
- confirma que el backend esté en el puerto `8080`.

### Login devuelve 401

- verifica usuario y contraseña;
- comprueba que el backend esté conectado a la base correcta;
- para desarrollo prueba `admin/admin123`;
- revisa la respuesta en la consola del backend.

### Login devuelve 403 en funciones de cliente

El usuario autenticado no tiene el permiso requerido por el endpoint. El usuario `admin` sirve para pruebas administrativas, pero el flujo real del cliente necesita un usuario/rol con permisos `clientes:*` y una entidad de cliente asociada.

### Expo no encuentra el teléfono

- conecta ambos dispositivos a la misma Wi-Fi;
- desactiva temporalmente VPN si interfiere;
- prueba `npx expo start --tunnel`;
- permite Node.js en el firewall.

### El emulador Android no alcanza `localhost:8080`

Usa `10.0.2.2` como host del backend.

### Cambié `.env` y Expo conserva el valor anterior

Reinicia Expo limpiando caché:

```powershell
npx expo start --clear
```

## 21. Reglas de seguridad

- Nunca guardes credenciales de PostgreSQL en la app móvil.
- Nunca incluyas `JWT_SECRET` en variables `EXPO_PUBLIC_`.
- Toda autorización debe validarse en el backend.
- No confíes en precios, totales, permisos ni IDs enviados por el cliente.
- Usa HTTPS en producción.
- Protege los tokens en almacenamiento seguro.
- No registres contraseñas ni tokens completos en consola.
- Cambia las credenciales predeterminadas antes de producción.

## 22. Comandos rápidos

```powershell
# Instalar
cd C:\restaurant-management-system\mobile
npm ci

# Comprobar
npx tsc --noEmit
npx expo-doctor

# Ejecutar
npm start

# Android
npm run android

# Web
npm run web

# Limpiar caché de Expo
npx expo start --clear
```

## 23. Estado final del análisis

La aplicación tiene una estructura clara, navegación funcional, componentes visuales reutilizables, carrito en memoria y una base de autenticación y servicios HTTP. Es adecuada como prototipo visual avanzado.

Todavía no puede considerarse integrada de extremo a extremo: el catálogo y los pedidos están simulados, la URL está fija, el contrato del menú requiere adaptación y no existe checkout. La prioridad técnica es conectar un menú real con un contrato acordado y luego completar el flujo autenticado de creación y confirmación de pedidos.
