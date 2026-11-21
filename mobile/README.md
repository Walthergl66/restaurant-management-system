# Estación Burger — App móvil

App de cliente (Expo SDK 58 + React Native + Expo Router) del
sistema de gestión de restaurante. Consume la API REST del
backend (`/api/v1`) **sin modificarlo**: el backend es el
contrato, la app se adapta a él.

## Estructura modular

Misma organización que el frontend web: **`features/` + `shared/`**, cada
feature con un barrel (`index.ts`) como única superficie pública, rutas
`app/` finas y los imports con el alias `@/` (mapea a `src/`).

```
src/
├── app/                        # Expo Router (cada archivo = ruta fina)
│   ├── (tabs)/                 # Menú, Carrito, Pedidos, Perfil
│   ├── (waiter)/               # Mesero: pestañas Mesas, Cocina y Listos; armado de pedido y producto
│   ├── (kitchen)/              # Cocina (rol COCINA): pestañas Comandas y Mostrador
│   ├── checkout.tsx            # Flujo de pedido idempotente
│   ├── login.tsx               # Auth
│   └── product/[id].tsx        # Detalle (reexport del barrel del feature)
├── components/                 # UI compartida (Header, Cards, ChipTabs, ui/* incl. QuantityStepper)
├── config/
│   └── environment.ts          # API_BASE_URL (ver "Conexión")
├── shared/                     # Infraestructura transversal, sin negocio
│   ├── hooks/useSondeo.ts      # Sondeo periódico mientras la pantalla está enfocada
│   ├── api/apiClient.ts        # Único boundary HTTP + inyección JWT
│   ├── api/apiError.ts         # ApiError ← ProblemDetail (RFC 7807)
│   └── storage/authStorage.ts  # Sesión en AsyncStorage
├── features/                   # Módulos por capacidad (cada uno con barrel)
│   ├── auth/                   # index.ts · api.ts · AuthContext · types · PerfilSheet
│   ├── menu/                   # index.ts · api.ts · components/ProductoScreen
│   ├── pedidos/                # index.ts · api.ts · idempotencia · estadoSocket
│   ├── carrito/                # index.ts · CartContext (estado local) · types
│   ├── clientes/               # index.ts · api.ts · types
│   ├── cocina/                 # index.ts · api.ts · agrupar · components/CocinaScreen
│   ├── mostrador/              # index.ts · api.ts · unir · components/PedidosListosScreen
│   └── mesero/                 # index.ts · api.ts · zonas · geometriaMesa · components/*
└── theme/                      # colores y tipografía
```

Reglas:
- Cada feature expone su API pública por `index.ts` (barrel). Cruzarlo
  se hace **siempre** por el barrel, nunca por una ruta interna.
- Imports internos con el alias `@/` (mapea a `src/`), p. ej.
  `@/shared/api/apiClient` o `@/features/menu`.
- Las rutas `app/` son finas: montan la pantalla del feature
  (`export { ProductoScreen as default } from '@/features/menu'`).
- La UI **nunca** hace `fetch` directo: todo pasa por
  `shared/api/apiClient.ts`.
- El boundary HTTP de cada feature vive en `features/<x>/api.ts`
  (antes `*Service.ts`).
- Los tipos viven junto al feature y nombran al DTO/SPI del
  backend que representan (ej. `PedidoCliente` = `PedidoClienteSPI`).
- El carrito es estado local; se envía al backend solo en el
  checkout.

## Conexión al backend

### Acceso móvil

El login presenta el logo sobre el fondo del mostrador y un formulario crema
con un borde inclinado ámbar y un título manuscrito, en continuidad con la web.
Permite ingresar con
correo o usuario y contraseña, mostrar la contraseña, recuperar el acceso y
crear una cuenta. La entrada inicia a oscuras, enciende el logo de neón y hace
parpadear dos veces el foco sobre el mostrador antes de revelar el formulario;
el foco queda encendido y el formulario aparece con la paleta crema y ámbar.
Si el dispositivo solicita reducir movimiento, se muestra la pantalla final
sin animación.
Durante el ingreso, el botón muestra un indicador de carga.

Copia `mobile/.env.example` a `mobile/.env`. En Expo Go, la IP del servidor se detecta automáticamente, así que no hace falta definir la URL. Para builds standalone configura `EXPO_PUBLIC_API_URL` con una dirección accesible desde el teléfono. `EXPO_PUBLIC_MAPBOX_TOKEN` se usa para mapas; es un valor público que queda dentro de la app. No pongas secretos del backend en el móvil.

La URL base se resuelve así (en orden):

1. `EXPO_PUBLIC_API_URL` en `mobile/.env` (variable de entorno, al bundlear)
2. Host del dev server de Expo (`hostUri`, en desarrollo) → la IP del PC
3. `extra.apiUrl` de `app.json` (builds sin dev server)
4. `http://localhost:8080/api/v1` (default)

El punto 2 es el que evita el error `Sin conexión / no se pudo
conectar al servidor` en un teléfono físico: `localhost` en el
dispositivo **es el propio teléfono**, no la PC. Como el teléfono ya
cargó el bundle desde el dev server, la IP de ese mismo `hostUri`
siempre es alcanzable, y `API_BASE_URL` se arma a partir de ella sin
configurar nada a mano.

```bash
# Copia mobile/.env.example a mobile/.env; la IP se detecta automáticamente.
# Teléfono en la misma red (Expo Go): basta con escanear el QR. Override manual solo si el backend está en otra máquina/puerto:
EXPO_PUBLIC_API_URL=http://192.168.2.100:8080/api/v1 npx expo start

# Emulador Android: Expo hace `adb reverse` solo del 8081 (bundle),
# así que el 8080 del backend hay que mapearlo aparte:
adb reverse tcp:8080 tcp:8080
npx expo start --android
```

### Cómo escanear el QR (y por qué a veces abre el navegador)

El QR que imprime `npx expo start` **no** contiene la app: contiene la
página intermedia `http://<IP-del-PC>:8081/_expo/loading`, que decide
en qué runtime abrir el proyecto. Por eso la cámara del teléfono la
abre en el **navegador** — es el comportamiento esperado.

Dos formas de llegar a la app:

1. Escanear con la cámara → se abre el navegador → pulsar
   **"Open in Expo Go"** → la app arranca con `exp://<IP>:8081`.
2. Escanear desde **dentro de la app Expo Go** (su lector de QR), que
   salta el navegador y abre directo.

> **No instales `expo-dev-client` en este proyecto.** En cuanto aparece
> en `package.json`, el CLI de Expo entra en modo "development build",
> la página intermedia deja de mostrarse y el QR pasa a contener una
> URL `http://localhost:8081` (no `exp://`), que en el teléfono apunta
> al propio teléfono: el navegador abre y no carga nada. Este proyecto
> se depura con Expo Go; una development build requiere `eas build` o
> `npx expo run:android` (Android SDK + JDK 17).

Requisitos: backend arriba en `:8080` y PostgreSQL accesible. El servidor de base de datos es externo; en desarrollo puedes iniciar backend y web desde la raíz con `docker compose --env-file backend/.env --env-file frontend/.env up --build -d`.

Si la app corre en el teléfono pero sigue sin conectar, revisa el
firewall del PC: el puerto `8080/tcp` debe estar abierto en la zona
del firewall donde está la interfaz por la que llega el teléfono
(`firewall-cmd --zone=<zona> --list-all`).

Builds standalone (APK/IPA): no hay dev server; define `EXPO_PUBLIC_API_URL`
en `mobile/.env` con una URL accesible desde el teléfono, o configura `extra.apiUrl`
en `app.json`. El token Mapbox puede definirse como `EXPO_PUBLIC_MAPBOX_TOKEN`
en el mismo archivo; es configuración pública incluida en el cliente. No pongas
secretos del backend en el entorno móvil.


## Pantalla del mesero

El mesero (permiso `pedidos:crear`) entra directamente a su grupo de rutas `(waiter)`:

1. **Seleccionar mesa.** Las mesas se agrupan en pestañas por **zona**, en el orden que define el
   administrador en la web (las zonas sin mesas habilitadas no se muestran). Cada mesa se dibuja con
   sus sillas según su capacidad y una mesa **ocupada** (`mesa.estado = OCUPADA`, mientras tenga una cuenta abierta) lleva un
   check. Se actualiza cada 30 s y con *pull to refresh*. Una mesa ocupada sigue
   siendo seleccionable: el pedido se suma a su cuenta. El ícono de perfil abre el panel con el
   nombre, el correo y el rol, y el botón **Cerrar sesión**.
2. **Pedido · Mesa N.** Pestañas de categoría, lista de productos con stepper −/+ (agrega sin
   personalización) y chevron para elegir extras e ingredientes. Una barra inferior muestra
   "N productos seleccionados · Ver pedido".
3. **Resumen.** Hoja inferior con las líneas (editar, duplicar, eliminar), notas, total y
   **Confirmar y enviar a cocina**. Si falla algún paso del envío, el borrador se cancela para no
   dejar un pedido abierto que bloquee el cobro de la mesa. Al salir con productos sin enviar se pide
   confirmación.

La lógica pura se prueba con `node --test` (por ejemplo `src/features/mesero/zonas.test.mjs`,
`geometriaMesa.test.mjs` y `enviarPedido.test.mjs`).

## Pantalla de inicio según el usuario

Al iniciar sesión, el grupo de rutas se decide **por permiso** (no por nombre de rol), con esta prioridad:

| Prioridad | Permiso | Grupo |
|---|---|---|
| 1 | `delivery:mis-entregas` | `(delivery)` — repartidor |
| 2 | `pedidos:crear` | `(waiter)` — mesero |
| 3 | `pedidos:estado-preparacion` | `(kitchen)` — cocina |
| 4 | cualquier otro | `(tabs)` — cliente |

El mesero también tiene permisos de cocina, por eso el orden: va a su pantalla y desde ella accede a la
pestaña Cocina. Quien tenga rol `COCINA` (sin `pedidos:crear`) entra directo a la pantalla de cocina. Quien
esté en el grupo de otro tipo de usuario es redirigido al suyo.

## Pantalla de cocina

Muestra las comandas de `GET /comandas?activas=true` y se actualiza cada 8 s y con *pull to refresh*; al llegar
comandas pendientes nuevas vibra brevemente.

- **Filtro por área** de preparación (Todas, Cocina, Barra…) y pestañas **Pendientes / Preparando / Listos**
  con su contador.
- Cada tarjeta indica el número de comanda, el origen (**Mesa N**, **Retiro**, **Domicilio** o *Pedido app*), el
  área, el código del pedido y las líneas con extras, ingredientes removidos y observaciones.
- Acciones: **Preparar**, **Listo** (desde Pendiente encadena preparación y listo) y **Enterado** para las
  comandas de **cancelación** (aviso de que una anulación aprobada cambia lo que se preparaba), que se muestran
  primero y en rojo.
- Si una acción falla (por ejemplo, sin conexión) se avisa y la tarjeta no cambia: el backend es la fuente de verdad.
- La pantalla es la misma para el rol COCINA (con perfil y cierre de sesión) y para la pestaña Cocina del
  mesero. *Pendiente:* contador de pendientes en el ícono de la pestaña.

## Mostrador: entrega de pedidos listos

Pantalla compartida donde se entrega lo que cocina ya dejó listo. Se llama **Listos** en la app del mesero y
**Mostrador** en la de cocina, y se actualiza cada 15 s y con *pull to refresh*.

| Quién | Qué ve |
|---|---|
| Mesero | Pedidos de **sus mesas** listos (botón *Entregar a la mesa*) y pedidos de la app para **retirar** listos (*Entregar al cliente*). |
| Cocina | Solo los pedidos de la app para **retirar** (*Entregar al cliente*). |

- Cada tarjeta muestra el código, "Mesa N" o "Retiro · nombre del cliente" y las líneas del pedido. Antes de
  registrar la entrega se pide confirmación.
- La entrega de retiro **no lleva PIN** (el cliente está presente). Los pedidos **a domicilio** no aparecen
  aquí: los entrega el repartidor (*pendiente de decisión*).
- Si una de las dos listas no se puede cargar (por ejemplo, un permiso que falta) se muestra la otra con un
  aviso; si falla una entrega, la tarjeta no cambia.
- Usa `GET /pedidos?estado=LISTO`, `POST /pedidos/{codigo}/entregar`,
  `GET /clientes/operacion/pedidos?estado=LISTO&entrega=RETIRAR` y `POST /clientes/pedidos/{codigo}/entregar`.

## Endpoints que consume

Las respuestas SPI y DTO web de pedidos pueden convertirse al modelo de
presentación común mediante `features/pedidos/normalizarPedido.ts`. El adaptador
normaliza nombres, mayúsculas, moneda y fechas, y rechaza datos inconsistentes.
Su tabla de mapeo y casos de prueba están documentados en
[`docs/api/patrones-traduccion-pedidos.md`](../docs/api/patrones-traduccion-pedidos.md).

| Pantalla | Endpoint | Auth |
| --- | --- | --- |
| Menú / detalle | `GET /api/v1/menu` | público |
| Login / refresh / logout / me | `/api/v1/auth/*` | login: no; resto: sí |
| Historial / carrito / dirección | `/api/v1/clientes/*` | sí |
| Checkout | `POST /clientes/pedidos` → `POST /clientes/pedidos/{codigo}/confirmar` | sí |
| Mesero: mesas y zonas | `GET /mesas` | sí |
| Mesero: enviar pedido | `POST /pedidos` → `POST /pedidos/{codigo}/items` → `POST /pedidos/{codigo}/confirmar` (si falla, `DELETE /pedidos/{codigo}`) | sí |
| Cocina / pestaña Cocina | `GET /comandas?activas=true`, `GET /areas`, `POST /comandas/{id}/en-preparacion`, `POST /comandas/{id}/listo` | sí |
| Mostrador / pestaña Listos | `GET /pedidos?estado=LISTO`, `POST /pedidos/{codigo}/entregar`, `GET /clientes/operacion/pedidos`, `POST /clientes/pedidos/{codigo}/entregar` | sí |

Notas de contrato:
- Dinero: el backend serializa `BigDecimal` como **número** JSON.
- Errores: ProblemDetail (RFC 7807) con `fieldErrors` como mapa
  campo→mensaje; la app los mapea a `ApiError`.
- Idempotencia (RF-24/25): crear y confirmar usan la misma
  `idempotencyKey`; reintentos no duplican pedidos.
- Estado en vivo del pedido: el backend empuja vía WebSocket
  (`/topic/pedido/{codigo}`, RF-43); hoy la app hace sondeo
  ligero cada 15s mientras hay pedidos activos.

## Comandos

```bash
npm install           # instala las dependencias fijadas en package-lock.json
npx expo start --clear # limpia la caché y abre el servidor de desarrollo
npx tsc --noEmit      # typecheck
```

Expo Go debe ser compatible con el SDK del proyecto. Si aparece el mensaje
"Project is incompatible with this version of Expo Go", actualiza Expo Go
desde la tienda y vuelve a iniciar el servidor con `npx expo start --clear`.
Este proyecto usa SDK 58. Para alinear dependencias al SDK declarado, ejecuta
`npx expo install --fix` y luego `npx expo-doctor`.

Para generar un build nativo Android, reemplaza el valor de ejemplo de
`android.config.googleMaps.apiKey` en `app.json` por una clave restringida por
nombre de paquete y SHA-1, suministrada fuera del repositorio. El campo de API
key no se necesita para abrir el proyecto en Expo Go.

## Pendientes

- Guardia de auth: sin sesión, las pantallas protegidas muestran
  el error 401 del backend (el login es un modal). Falta un
  gate en el layout raíz.
- WebSocket (RF-43) para estado en vivo real del pedido.
- `GET /clientes/direcciones` no existe en el backend: en
  domicilio se registra la dirección en cada checkout.
