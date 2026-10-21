# Estación Burger — App móvil

App de cliente (Expo SDK 57 + React Native + Expo Router) del
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
│   ├── (waiter)/               # Mesero: atención de mesas, producto
│   ├── checkout.tsx            # Flujo de pedido idempotente
│   ├── login.tsx               # Auth
│   └── product/[id].tsx        # Detalle (reexport del barrel del feature)
├── components/                 # UI compartida (Header, Cards, ui/*)
├── config/
│   └── environment.ts          # API_BASE_URL (ver "Conexión")
├── shared/                     # Infraestructura transversal, sin negocio
│   ├── api/apiClient.ts        # Único boundary HTTP + inyección JWT
│   ├── api/apiError.ts         # ApiError ← ProblemDetail (RFC 7807)
│   └── storage/authStorage.ts  # Sesión en AsyncStorage
├── features/                   # Módulos por capacidad (cada uno con barrel)
│   ├── auth/                   # index.ts · api.ts · AuthContext · types
│   ├── menu/                   # index.ts · api.ts · components/ProductoScreen
│   ├── pedidos/                # index.ts · api.ts · idempotencia · estadoSocket
│   ├── carrito/                # index.ts · CartContext (estado local) · types
│   ├── clientes/               # index.ts · api.ts · types
│   └── mesero/                 # index.ts · api.ts · components/AtencionMesasScreen
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

La URL base se resuelve así (en orden):

1. `EXPO_PUBLIC_API_URL` (variable de entorno, al bundlear)
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
# Teléfono en la misma red (Expo Go): basta con escanear el QR,
# la IP se toma sola. Override manual solo si el backend está en otra máquina/puerto:
EXPO_PUBLIC_API_URL=http://192.168.2.100:8080/api/v1 npx expo start

# Emulador Android: Expo hace `adb reverse` solo del 8081 (bundle),
# así que el 8080 del backend hay que mapearlo aparte:
adb reverse tcp:8080 tcp:8080
npx expo start --android
```

Requisitos: backend arriba en `:8080` y PostgreSQL
(`docker compose up -d postgres` desde la raíz del repo).

Si la app corre en el teléfono pero sigue sin conectar, revisa el
firewall del PC: el puerto `8080/tcp` debe estar abierto en la zona
del firewall donde está la interfaz por la que llega el teléfono
(`firewall-cmd --zone=<zona> --list-all`).

Builds standalone (APK/IPA): no hay dev server, así que hay que fijar
la URL en build con `EXPO_PUBLIC_API_URL=...` o en `app.json`.


## Endpoints que consume

| Pantalla | Endpoint | Auth |
| --- | --- | --- |
| Menú / detalle | `GET /api/v1/menu` | público |
| Login / refresh / logout / me | `/api/v1/auth/*` | login: no; resto: sí |
| Historial / carrito / dirección | `/api/v1/clientes/*` | sí |
| Checkout | `POST /clientes/pedidos` → `POST /clientes/pedidos/{codigo}/confirmar` | sí |

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
npx expo start        # servidor de desarrollo
npx tsc --noEmit      # typecheck
```

## Pendientes

- Guardia de auth: sin sesión, las pantallas protegidas muestran
  el error 401 del backend (el login es un modal). Falta un
  gate en el layout raíz.
- WebSocket (RF-43) para estado en vivo real del pedido.
- `GET /clientes/direcciones` no existe en el backend: en
  domicilio se registra la dirección en cada checkout.
