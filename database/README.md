# Módulo de Base de Datos — PostgreSQL

Este directorio contiene las herramientas, documentación y scripts de utilidad para la administración de la base de datos PostgreSQL del Sistema de Gestión de Restaurante.

---

## 🗄️ Información General

- **Motor:** PostgreSQL 16+
- **Migraciones:** Administradas automáticamente por **Flyway** en el arranque del backend (ubicadas en `backend/src/main/resources/db/migration/`).
- **Esquema:** Monolito modular con soporte para dinero de alta precisión (`NUMERIC(12,2)`), auditoría append-only y secuencias globales para comandas y comprobantes.

---

## Regenerar el esquema de referencia

`database/schema/restaurante_completo.sql` es un volcado de solo estructura. Las migraciones Flyway
son la fuente de verdad; regenera el volcado cada vez que agregues una migración.

1. Crea una base desechable (usa la misma versión mayor de PostgreSQL que tu base real):
   ```powershell
   docker run -d --name pg-esquema -e POSTGRES_PASSWORD=dev -e POSTGRES_DB=restaurante -p 5433:5432 postgres:17
   ```
2. Arranca el backend contra ella, en la misma terminal, para que Flyway aplique las migraciones.
   Comprueba antes que `$env:DB_PORT` sea `5433`; si no, migrarías tu base real:
   ```powershell
   cd backend
   $env:DB_HOST="localhost"; $env:DB_PORT="5433"; $env:DB_NAME="restaurante"
   $env:DB_USER="postgres"; $env:DB_PASSWORD="dev"; $env:DB_SSLMODE="disable"
   ./mvnw spring-boot:run
   ```
   Espera `Successfully applied N migrations` y detén el backend con `Ctrl+C`.
3. Vuelca el esquema y cópialo al repositorio (no uses `>`: en Windows PowerShell 5 guarda UTF-16):
   ```powershell
   cd ..
   docker exec pg-esquema pg_dump -U postgres -d restaurante --schema-only --no-owner --no-privileges "--exclude-table=*.flyway_schema_history" -f /tmp/esquema.sql
   docker cp pg-esquema:/tmp/esquema.sql database\schema\restaurante_completo.sql
   ```
4. Quita las líneas `\restrict`/`\unrestrict` (llevan un token distinto en cada ejecución) y añade la cabecera
   con la nueva versión de migración:
   ```powershell
   $ruta = "database\schema\restaurante_completo.sql"
   $lineas = Get-Content $ruta -Encoding UTF8 | Where-Object { $_ -notmatch '^\\(un)?restrict ' }
   $cabecera = @(
     "-- Esquema generado con pg_dump --schema-only tras la migración V<N> (<fecha>).",
     "-- Referencia de lectura: la fuente de verdad son las migraciones de backend/src/main/resources/db/migration.",
     "-- No editar a mano; se regenera con cada migración nueva (ver database/README.md).",
     ""
   )
   ($cabecera + $lineas) | Set-Content $ruta -Encoding UTF8
   ```
5. Cuenta schemas y tablas, y actualiza el conteo del `README.md` raíz (sección Base de Datos):
   ```powershell
   docker exec pg-esquema psql -U postgres -d restaurante -tAc "select count(distinct table_schema), count(*) from information_schema.tables where table_type='BASE TABLE' and table_schema not in ('public','pg_catalog','information_schema')"
   ```
6. Limpia el contenedor y las variables de la terminal:
   ```powershell
   docker rm -f pg-esquema
   Remove-Item Env:DB_HOST, Env:DB_PORT, Env:DB_NAME, Env:DB_USER, Env:DB_PASSWORD, Env:DB_SSLMODE
   ```

---

## ⚙️ Configuración y Credenciales

Las credenciales de acceso de la base de datos se definen en el archivo `backend/.env` (o plantilla `backend/.env.example`):

```env
DB_HOST=host.docker.internal # BD en el equipo local desde Docker; o DNS/IP remoto
DB_PORT=5432
DB_NAME=restaurante
DB_USER=restaurante
DB_PASSWORD=restaurante
DB_SSLMODE=require
```

Compose inicia el backend (y en desarrollo también la web) y se conecta a esta instancia
externa; no crea ni administra un servidor PostgreSQL. Usa `docker compose --env-file backend/.env --env-file frontend/.env up --build -d` para desarrollo. La base debe existir y el usuario necesita permisos para que Flyway aplique las migraciones al arrancar. La conexión requiere TLS por defecto (`DB_SSLMODE=require`); configura `disable` si el servidor no ofrece TLS. Backend y utilidades de base de datos comparten `backend/.env`; para Compose, pasa explícitamente archivos con `--env-file`. Al ejecutar el backend de forma nativa, `DB_HOST=localhost` sirve para una BD en la misma máquina.

Los scripts de este directorio (`backup.sh` y `restore.sh`) cargan `backend/.env` automáticamente. Puedes indicar otra ruta mediante `ENV_FILE`.

### Cliente de PostgreSQL

Los scripts usan las herramientas PostgreSQL del host (`pg_dump`, `psql` y
`pg_restore`) para conectarse por TCP al host configurado en `.env`, con las
credenciales indicadas. Instala el cliente PostgreSQL si esas herramientas no
están disponibles.

---

## 📦 Copias de Seguridad (Backups)

El script `backup.sh` genera un volcado binario en formato personalizado de PostgreSQL (`pg_dump -F c`), ideal para restauración rápida y portable.

### Uso

Desde la raíz del proyecto o desde este directorio:

```bash
./database/backup.sh [ruta_archivo_personalizada.dump]
```

Si no se especifica ruta, el archivo se crea automáticamente en:
`database/backups/restaurante_<DB_NAME>_<TIMESTAMP>.dump`

### Validación

El script valida automáticamente que el archivo generado sea legible e imprime el catálogo de tablas incluidas sin restaurar datos.

---

## 🔄 Restauración de Datos (Restore)

> ⚠️ **ATENCIÓN:** El proceso de restauración recrea la base de datos de destino (`DROP DATABASE` —con `WITH (FORCE)` para cerrar conexiones activas, p. ej. el backend corriendo— y `CREATE DATABASE`). Todos los datos existentes en la base seleccionada serán reemplazados por los del volcado.

### Uso

```bash
./database/restore.sh ./database/backups/restaurante_restaurante_20260929_120000.dump [nombre_bd_destino]
```

Si no se especifica `nombre_bd_destino`, se utilizará el valor de `DB_NAME` configurado en el entorno o `.env`.

---

## 📂 Estructura del Directorio

```
database/
├── backups/       # Directorio local para almacenamiento de dumps (ignorado por git)
├── backup.sh      # Script de generación de respaldos (PostgreSQL nativo o contenedor)
├── restore.sh     # Script de restauración integral
└── README.md      # Guía técnica del módulo de base de datos
```
