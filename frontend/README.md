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

---

## 🚀 Puesta en Marcha

### 1. Requisitos Previos

- **Node.js**: v20+ o v24+ LTS
- **npm** (o yarn / pnpm)
- **Backend**: debe estar corriendo en `http://localhost:8080` (vía Docker o `run-backend.sh`)

### 2. Configurar Variables de Entorno

Copia el archivo de ejemplo a `.env.local`:

```bash
cp .env.example .env.local
```

Contenido de `.env.local`:

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
