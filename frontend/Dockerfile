# ------------------------------------------------------------------
# Frontend Next.js (App Router) — build multi-stage con output standalone.
# next.config.ts usa `output: 'standalone'`, así que el runtime solo
# necesita la carpeta .next/standalone + .next/static + public.
# ------------------------------------------------------------------

# Stage 1: dependencias (cacheadas por package-lock)
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# Stage 2: build
FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
# IMPORTANTE: con `output: standalone`, los rewrites() de next.config.ts se
# evalúan en BUILD time, así que BACKEND_URL debe estar presente al compilar
# (si no, el rewrite queda horneado con el fallback localhost y el contenedor
# no encuentra el backend). Se recibe como build arg desde docker-compose.
ARG BACKEND_URL=http://backend:8080
ENV BACKEND_URL=${BACKEND_URL}
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# Stage 3: runtime mínimo, usuario no root
FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs

# Salida standalone: incluye el server.js y solo las deps necesarias.
COPY --from=build /app/.next/standalone ./
COPY --from=build /app/.next/static ./.next/static
COPY --from=build /app/public ./public

USER nextjs
EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
# server.js es el entrypoint que genera `output: standalone`.
CMD ["node", "server.js"]
