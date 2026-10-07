# Módulo de Base de Datos — PostgreSQL

Este directorio contiene las herramientas, documentación y scripts de utilidad para la administración de la base de datos PostgreSQL del Sistema de Gestión de Restaurante.

---

## 🗄️ Información General

- **Motor:** PostgreSQL 16+
- **Migraciones:** Administradas automáticamente por **Flyway** en el arranque del backend (ubicadas en `backend/src/main/resources/db/migration/`).
- **Esquema:** Monolito modular con soporte para dinero de alta precisión (`NUMERIC(12,2)`), auditoría append-only y secuencias globales para comandas y comprobantes.

---

## ⚙️ Configuración y Credenciales

Las credenciales de acceso de la base de datos se definen en el archivo `backend/.env` (o plantilla `backend/.env.example`):

```env
DB_HOST=localhost          # o la IP del servidor de BD (ej. 192.168.1.230)
DB_PORT=5432
DB_NAME=restaurante
DB_USER=restaurante
DB_PASSWORD=restaurante
```

Los scripts de este directorio (`backup.sh` y `restore.sh`) detectan y cargan automáticamente las variables desde `backend/.env`.

### Cliente de PostgreSQL

Ninguno de los dos scripts requiere PostgreSQL instalado en el host:
- Si el contenedor `restaurante-postgres` (el del `docker compose up -d`) está corriendo, ejecutan el cliente pg **dentro** de ese contenedor.
- Si no, usan el `pg_dump`/`psql`/`pg_restore` del host como fallback.
- Con `DB_HOST=localhost` (default del stack Docker) el destino es la BD del compositor, por socket dentro del contenedor. Si `DB_HOST` es otra cosa, conectan por TCP a ese host remoto con las credenciales de `.env` (p. ej. para respaldar la BD de producción).

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
