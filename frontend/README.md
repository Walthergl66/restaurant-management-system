# Frontend — Sistema de Gestión de Restaurante

Aplicación web desarrollada en **Next.js 16** con **React 19**, **TypeScript** y **Tailwind CSS**, diseñada para la operativa del restaurante: salones, mesas, pedidos en tiempo real, cocina (KDS), caja, cuentas, anulaciones, reportes y administración.

El frontend se ejecuta directamente en el entorno de desarrollo del cliente con Node.js y se comunica con la API REST del backend en `http://localhost:8080`.

---

## 🛠️ Stack Tecnológico

- **Framework:** Next.js 16 (App Router)
- **UI & Estado:** React 19, Zustand (estado global), TanStack React Query (caché y sincronización de datos)
- **Estilos:** Tailwind CSS v4, Lucide React (iconografía)
- **Cliente HTTP:** Axios (con rewrites de `/api/*` hacia el backend en `next.config.ts`)
- **Visualización:** Recharts (reportes y métricas)
- **Notificaciones:** React Hot Toast

---

## 📁 Estructura del Proyecto

La arquitectura es **modular por feature**. Cada dominio de negocio vive en
`src/features/<feature>/` y expone su superficie pública solo a través de su barrel
`index.ts`. El código transversal (cliente HTTP, layout, UI, utilidades) vive en
`src/shared/`. Las rutas de Next.js son finas: solo montan la pantalla de la feature.

```
frontend/
├── src/
│   ├── app/                      # Rutas de Next.js (App Router) — solo montan pantallas
│   │   ├── (dashboard)/          # Vistas protegidas (layout con Sidebar/TopBar + AuthGuard)
│   │   │   ├── anulaciones/      # Aprobación y consulta de anulaciones
│   │   │   ├── auditoria/        # Registro de eventos auditables
│   │   │   ├── caja/             # Apertura, movimientos y cierre de caja
│   │   │   ├── cocina/           # Pantalla de comandas en preparación (KDS)
│   │   │   ├── cuentas/          # Consulta y cierre de cuentas
│   │   │   ├── dashboard/        # Resumen general del turno
│   │   │   ├── mesas/            # Mapa y estados de mesas
│   │   │   ├── pedidos/          # Toma de pedidos presenciales
│   │   │   ├── productos/        # Catálogo de productos y categorías
│   │   │   ├── reportes/         # Reportes financieros y ventas
│   │   │   └── usuarios/         # Gestión de usuarios y roles
│   │   └── login/                # Autenticación y obtención de JWT
│   ├── features/                 # Módulos de dominio (uno por feature)
│   │   └── <feature>/            # p. ej. pedidos, catalogo, auth, caja...
│   │       ├── index.ts          # Barrel: ÚNICA superficie pública del módulo
│   │       ├── api.ts            # Llamadas HTTP tipadas (axios, vía shared/api)
│   │       ├── hooks.ts          # Hooks de TanStack Query / mutaciones
│   │       ├── types.ts          # Tipos del dominio de la feature
│   │       ├── components/       # Pantallas y componentes de la feature
│   │       └── lib/              # Utilidades internas (opcional)
│   └── shared/                   # Código transversal, sin dependencia de features*
│       ├── api/                  # Cliente axios, queryClient
│       ├── layout/               # Providers, Sidebar, TopBar
│       ├── ui/                    # Componentes UI reutilizables (Logo...)
│       ├── lib/                   # Utilidades (permisos, formato...)
│       └── types/                 # Tipos comunes
├── next.config.ts                # Configuración de proxy/rewrites hacia backend
└── package.json                  # Dependencias y scripts npm
```

### Reglas de la arquitectura

- **Imports entre módulos solo por el barrel.** Entre features (y desde `app/`) se
  importa desde `@/features/<feature>`, nunca desde rutas profundas como
  `@/features/<feature>/components/...` o `.../api`.
- **Rutas finas.** Los archivos de `app/` reexportan la pantalla del barrel, p. ej.
  `export { DashboardScreen as default } from '@/features/dashboard'`.
- **Estado separado.** El estado de servidor va en TanStack Query (hooks por feature);
  el estado de UI/sesión va en Zustand (p. ej. `features/auth/store.ts`). Nunca se
  duplica la respuesta del servidor en un store.
- **Sin fetching en componentes.** Los componentes consumen hooks/API de su feature,
  nunca el cliente HTTP directo.

\* `shared/layout/Sidebar` y `TopBar` leen la sesión desde `@/features/auth` (única
excepción documentada de la dirección `shared → feature`).

### Dashboard operativo

El dashboard combina los pedidos recientes con el estado y apertura inicial de la
caja del turno, métricas de ventas y métodos de pago, una gráfica de ventas de los
últimos siete días y los productos más vendidos. Los pedidos enlazan a su detalle;
los accesos de operación llevan a pedidos y caja. Los datos se obtienen mediante
hooks de reportes, pedidos y caja y muestran estados de carga y vacío. La autorización
por permisos que envuelve la pantalla se conserva.
En anchos de hasta 1150 px, las tarjetas operativas y analíticas pasan a una columna
y las métricas se organizan en dos columnas para mantener espacio legible junto a la
navegación lateral.

El encabezado del área autenticada abarca el ancho de la ventana e integra el logo,
el título contextual del dashboard y el menú de cuenta. Debajo, la navegación lateral
agrupa accesos por Operación, Administración y Control con iconos y etiquetas; no
repite el logo y conserva el filtrado por permisos. El espacio de trabajo compacta
su composición en pantallas pequeñas. En escritorio amplio, el encabezado limita el
espacio de sus zonas y trunca texto largo para ayudar a evitar solapamientos entre el
título y el menú de cuenta. En el dashboard, los
pedidos recientes se presentan como tickets visuales y los bloques de operación y
estadísticas usan iconografía para facilitar su lectura rápida.
La altura del encabezado usa límites fluidos: en escritorio amplio varía entre 104 y
128 px según el ancho; entre 901 y 1300 px mide 104 px, en tablet de 681 a 900 px
mide 88 px y en móvil conserva 82 px. El título y los controles de cuenta reducen su
escala para mantener el encabezado compacto; el logo usa casi toda la altura
disponible para permanecer reconocible.
El menú de cuenta ofrece perfil, configuración informativa y cierre de sesión. En
`/dashboard`, “Kitchen pass” y “Resumen operativo” aparecen en el encabezado solo
para usuarios con `reportes:ver`; los controles de cuenta siguen disponibles para
todos los usuarios autenticados. Escape cierra el menú y devuelve el foco al control
que lo abre.

### Identidad visual del área autenticada

Las rutas protegidas comparten el shell carbón con navegación ámbar y acentos
magenta. Las superficies de trabajo, tarjetas, formularios y tablas usan una paleta
crema con texto oscuro; los títulos principales llevan tipografía manuscrita y
subrayado ámbar. Los estados mantienen colores semánticos con tonos legibles sobre
crema, y los avisos y estados vacíos siguen el mismo tratamiento de superficie.
La cocina conserva su distinción entre pendientes, en preparación y listos mediante
acentos por estado dentro de paneles crema. El login mantiene su composición propia.

### Adaptación responsive de operaciones

El espacio autenticado ajusta su shell por ancho de pantalla: por debajo de 900 px
la navegación lateral se compacta a iconos y el contenido usa márgenes fluidos.
Las columnas de cocina, salón y reportes se apilan en vistas estrechas; los KPI de
mesas y cocina se reorganizan, y los formularios del modal de nuevo pedido apilan
sus campos en móvil. Las tablas conservan sus columnas y permiten desplazamiento
horizontal dentro de su contenedor cuando el ancho disponible no basta.

### Acceso web

La pantalla de acceso acepta correo o nombre de usuario y contraseña. En escritorio
presenta una composición dividida: un fondo fotográfico del pase (`public/login-brand-background.png`)
con el logo superpuesto como elemento independiente, junto a un formulario en tono crema con título manuscrito, campos claros y acción ámbar. En pantallas de hasta 680 px oculta la
fotografía y muestra el formulario compacto con el logo; hasta 380 px reduce los
márgenes. El acceso incluye control para mostrar u ocultar la contraseña, recuperación
que indica contactar a un administrador y mensajes en línea para campos incompletos
o errores de autenticación. Los campos y botones muestran foco de teclado; los controles
de contraseña y recuperación tienen áreas táctiles amplias.

---

## 🚀 Puesta en Marcha

### 1. Requisitos Previos

- **Node.js**: v20+ o v24+ LTS
- **npm** (o yarn / pnpm)
- **Backend**: debe estar corriendo en `http://localhost:8080` (vía Docker o `run-backend.sh`)

### 2. Configurar Variables de Entorno

Copia el archivo de ejemplo a `frontend/.env`:

```bash
cp .env.example .env
```

Contenido de `frontend/.env`:

```env
# Dirección del backend Spring Boot
BACKEND_URL=http://localhost:8080
```

### 3. Instalar Dependencias

```bash
npm install
```

### 4. Ejecutar en Modo Desarrollo

```bash
npm run dev
```

La aplicación estará disponible en [http://localhost:3000](http://localhost:3000).

### Ejecutar con Docker Compose

Desde la raíz, carga de forma explícita las variables de los componentes:
`docker compose --env-file backend/.env --env-file frontend/.env up --build -d`.
La web queda en `http://localhost:7070`; cambia `FRONTEND_PORT` en
`frontend/.env` si necesitas otro puerto. Para ejecución local, `BACKEND_URL`
en `frontend/.env` define el destino del rewrite. En Compose el destino interno
se configura como `http://backend:8080`. El proxy `/api/*` del frontend se
conecta al servicio `backend` dentro de la red de Compose. El servicio usa la
etapa `dev` de `frontend/Dockerfile`, monta `frontend/` dentro del contenedor y
conserva `node_modules` y `.next` en volúmenes Docker. Los cambios de código se
reflejan con la recarga de Next.js; al cambiar dependencias, ejecuta
`docker compose exec frontend npm ci` y reinicia el servicio frontend.

---

## 📜 Scripts Disponibles

- `npm run dev`: Inicia el servidor de desarrollo con hot reload en el puerto 3000.
- `npm run build`: Compila la aplicación para producción.
- `npm run start`: Inicia el servidor de producción compilado.
- `npm run lint`: Ejecuta el linter ESLint para comprobar estándares de código.

---

## 🔗 Comunicación con el Backend

El frontend utiliza la directiva `rewrites` en `next.config.ts`:

- Todas las peticiones al path `/api/*` son redirigidas automáticamente a `${BACKEND_URL}/api/*`.
- Esto evita problemas de CORS durante el desarrollo y unifica los endpoints bajo el mismo dominio de origen en el navegador.
