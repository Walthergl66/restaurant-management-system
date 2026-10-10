# Estación Burger — App móvil

App de cliente (Expo SDK 57 + React Native + Expo Router) del
sistema de gestión de restaurante. Consume la API REST del
backend (`/api/v1`) **sin modificarlo**: el backend es el
contrato, la app se adapta a él.

## Estructura modular

```
src/
├── app/                        # Expo Router (cada archivo = pantalla)
│   ├── (tabs)/                 # Menú, Carrito, Pedidos, Perfil
│   ├── checkout.tsx            # Flujo de pedido idempotente
│   ├── login.tsx               # Auth
│   └── product/[id].tsx        # Detalle de producto
├── components/                 # UI compartida (Header, Cards, ui/*)
├── config/
│   └── environment.ts          # API_BASE_URL (ver "Conexión")
├── core/                       # Infraestructura, sin lógica de negocio
│   ├── api/apiClient.ts        # Único boundary HTTP + inyección JWT
│   ├── api/apiError.ts         # ApiError ← ProblemDetail (RFC 7807)
│   └── storage/authStorage.ts  # Sesión en AsyncStorage
├── features/                   # Módulos por capacidad
│   ├── auth/                   # types + authService + AuthContext
│   ├── menu/                   # types (MenuDto) + menuService
│   ├── pedidos/                # types (SPI) + pedidosService + idempotencia
│   └── carrito/                # CartContext (estado local) + types
└── theme/                      # colores y tipografía
```

Reglas:
- La UI **nunca** hace `fetch` directo: todo pasa por
  `core/api/apiClient.ts`.
- Los tipos viven junto al feature y nombran al DTO/SPI del
  backend que representan (ej. `PedidoCliente` = `PedidoClienteSPI`).
- El carrito es estado local; se envía al backend solo en el
  checkout.

## Conexión al backend

La URL base se resuelve así (en orden):

1. `EXPO_PUBLIC_API_URL` (variable de entorno, al bundlear)
2. `extra.apiUrl` de `app.json`
3. `http://localhost:8080/api/v1` (default de desarrollo)

```bash
# Teléfono en la misma red (Expo Go):
EXPO_PUBLIC_API_URL=http://192.168.2.100:8080/api/v1 npx expo start

# Emulador Android (adb reverse, localhost funciona):
npx expo start --android
```

Requisitos: backend arriba en `:8080` y PostgreSQL
(`docker compose up -d postgres` desde la raíz del repo).

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
