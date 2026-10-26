# Variables de entorno para el servidor de archivos

Esta referencia acompaña a [la guía de Ubuntu](archivos-ubuntu.md). Explica qué
archivo editar, de dónde toma sus valores cada proceso y qué debes reiniciar
después de un cambio. Los `.example` son plantillas; la aplicación no los carga
directamente.

## 1. Qué archivo editar

| Ejecución | Archivo que debes editar | Responsabilidad |
| --- | --- | --- |
| Docker Compose local | `.env` en la raíz | Base de datos y variables del almacenamiento que interpola el compose. |
| Backend dentro de Docker | `backend/.env` | JWT, contraseña inicial y otras variables no sustituidas por `environment`. |
| Backend directamente en Windows/Linux | `backend/.env` | Conexión a PostgreSQL, SFTP, lectura HTTP y rutas locales de las claves. |
| Frontend Next.js | `frontend/.env.local` | `BACKEND_URL`, dirección del backend desde el proceso de Next.js. |

**Actualizar una plantilla no actualiza un `.env` existente.** Compara ambos e
incorpora las variables que necesites. No reemplaces tu archivo completo con la
plantilla si ya contiene contraseñas o un `JWT_SECRET` propio.

Las plantillas contienen valores de ejemplo y campos vacíos para secretos. No
incluyen una clave privada ni credenciales reales de tu instalación.

## 2. Precedencia al utilizar Docker

Hay dos pasos distintos:

1. **Compose sustituye `${VARIABLE}` en sus archivos YAML.** Usa el entorno de la
   terminal o, normalmente, el `.env` de la raíz. Un `--env-file` explícito puede
   seleccionar otro archivo para esta sustitución.
2. **Compose prepara el entorno del contenedor.** En este proyecto carga primero
   `backend/.env` y después `.env` de la raíz mediante `env_file`. Finalmente,
   los valores de `environment` prevalecen sobre los de esos archivos.

Por ejemplo, el compose base declara:

```yaml
ARCHIVOS_HOST: ${ARCHIVOS_HOST:-192.168.100.77}
ARCHIVOS_CLAVE_PRIVADA: /run/archivos/archivos_backend
```

Por eso, al usar Docker, cambia `ARCHIVOS_HOST` en el `.env` de la raíz. Una
modificación de ese mismo campo solo en `backend/.env` no sustituye el valor
de `environment`. La ruta privada está fijada a la ubicación dentro del
contenedor, aunque `backend/.env` contenga otra ruta para ejecución nativa.

`env_file: backend/.env` no significa que ese archivo sea automáticamente la
fuente de todas las expresiones `${...}` del YAML. Sigue los dos pasos anteriores
para determinar el valor efectivo.

El compose base también fija `DB_HOST=postgres`, el puerto interno de la aplicación
a `8080` y los paths de las claves bajo `/run/archivos/`. La dirección `localhost`
dentro del contenedor no representa a Windows ni a Ubuntu.

El complemento `docker-compose.archivos.yml` añade sus propias reglas:

- Activa las subidas con `ARCHIVOS_HABILITADO=true`.
- Usa `ARCHIVOS_DB_NAME` para elegir la base del backend.
- Fija el usuario SFTP a `archivosapp` y las claves a `/run/archivos/`.

Si necesitas otro usuario o montaje, adapta ese complemento o utiliza el compose
base con la configuración correspondiente. La guía habitual usa el compose base.

## 3. Variables del almacenamiento

| Variable | Ejemplo o valor por defecto | Función |
| --- | --- | --- |
| `ARCHIVOS_HABILITADO` | `false` | Habilita subidas, lecturas remotas y limpieza del módulo. Cambia a `true` después de preparar y comprobar Ubuntu. |
| `ARCHIVOS_HOST` | `192.168.100.77` | Dirección del servidor SSH/SFTP que alcanza el backend. No lleva `http://` ni una ruta. |
| `ARCHIVOS_PUERTO` | `22` | Puerto SSH/SFTP. No es el puerto HTTP de Nginx. |
| `ARCHIVOS_USUARIO` | `archivosapp` | Cuenta de Ubuntu que tiene autorizada la clave pública del backend. |
| `ARCHIVOS_CLAVE_PRIVADA` | Ruta absoluta del archivo privado | Identidad con la que el backend se autentica. En Docker se fija `/run/archivos/archivos_backend`. |
| `ARCHIVOS_KNOWN_HOSTS` | Ruta absoluta de `known_hosts` | Identidad verificada del servidor al que se conectará. En Docker se fija `/run/archivos/known_hosts`. |
| `ARCHIVOS_DIRECTORIO` | `/srv/restaurante/media` | Raíz física de almacenamiento dentro de Ubuntu. El backend añade `imagenes/` o `videos/`. |
| `ARCHIVOS_LECTURA_URL` | `http://192.168.100.77/media/` | Base HTTP de Nginx que el backend consulta para leer contenido. No es la URL pública de la API. |
| `ARCHIVOS_TIMEOUT` | `15s` | Espera de conexión y de operaciones de red. Usa una duración, por ejemplo `15s` o `30s`. |
| `ARCHIVOS_DB_NAME` | `restaurante_archivos` | Solo en el complemento de Compose: selecciona la base del backend en lugar de `DB_NAME`. |

La dirección de escritura y la de lectura deben resolver a un almacenamiento
coherente. Si el backend escribe `imagenes/UUID.png`, Nginx debe entregar ese
mismo archivo bajo `ARCHIVOS_LECTURA_URL + imagenes/UUID.png`.

Ejemplo de correspondencia:

```text
ARCHIVOS_DIRECTORIO=/srv/restaurante/media
    + imagenes/UUID.png
    = /srv/restaurante/media/imagenes/UUID.png

ARCHIVOS_LECTURA_URL=http://192.168.100.77/media/
    + imagenes/UUID.png
    = http://192.168.100.77/media/imagenes/UUID.png
```

El script de Ubuntu y la configuración de Nginx de la guía usan esa misma
estructura. Para cambiarla debes adaptar también las carpetas, permisos,
`alias` y el directorio inicial de SFTP del script. Cambiar solo el `.env`
del backend no cambia archivos de configuración dentro de Ubuntu.

Los límites de 10 MiB por imagen y 100 MiB por video están definidos en el
validador, el formulario y la configuración multipart; no son variables
`ARCHIVOS_*` configurables actualmente.

## 4. Variables de base de datos y puertos

| Variable | Uso en desarrollo local |
| --- | --- |
| `DB_NAME` | Base que selecciona el backend del compose base. En un volumen nuevo también define la base inicial de PostgreSQL. |
| `DB_USER` | Usuario PostgreSQL; debe coincidir con la instalación del volumen existente. |
| `DB_PASSWORD` | Contraseña PostgreSQL. Usa una propia y mantenla coherente entre los archivos del entorno que utilices. |
| `DB_PORT` | Puerto interno al que llega el backend Docker. Mantén `5432` para el PostgreSQL del compose actual. |
| `DB_HOST_PORT` | Puerto publicado en Windows para llegar a PostgreSQL; puede diferir del interno. |
| `DB_HOST` | En ejecución nativa, host de PostgreSQL. El compose local lo fija a `postgres` para su backend. |
| `DB_SSLMODE` | `disable` para PostgreSQL local del compose; usa la configuración TLS apropiada para una base remota. |
| `BACKEND_PORT` | Puerto que publica el compose para el backend en Windows; por defecto `8080`. |
| `SERVER_PORT` | Puerto del backend nativo. Dentro del compose local se fija `8080`. |
| `SPRING_PROFILES_ACTIVE` | Perfil `dev` o `prod`. El compose local lo pasa al contenedor y usa `dev` por defecto. |

Si publicas PostgreSQL con `DB_HOST_PORT=5433`, el backend de Docker sigue
conectándose a `postgres:5432`; un backend nativo en Windows debe usar
`DB_HOST=localhost` y `DB_PORT=5433` en `backend/.env`.

Las variables de inicialización `POSTGRES_*` del contenedor no recrean bases ni
cambian contraseñas automáticamente cuando ya existe su volumen. Crear otra base
o cambiar un usuario existente son operaciones de PostgreSQL independientes.

## 5. Secretos y frontend

| Variable | Dónde configurarla | Observación |
| --- | --- | --- |
| `JWT_SECRET` | `backend/.env` en el compose local | Secreto aleatorio del backend. No es una clave SSH y no debe llegar al frontend. |
| `ADMIN_INITIAL_PASSWORD` | `backend/.env` en el compose local | Contraseña inicial de la aplicación; solo se consume para crear `admin` si no existe. |
| `BACKEND_URL` | `frontend/.env.local` | URL sin `/api/v1`, por ejemplo `http://localhost:8080`. Es accesible desde Next.js. |

El formulario envía sus archivos al backend con la sesión del usuario de la
aplicación. `BACKEND_URL` no debe apuntar a Nginx; la conexión a Nginx la realiza
Java usando `ARCHIVOS_LECTURA_URL`.

El frontend y el móvil no necesitan `ARCHIVOS_CLAVE_PRIVADA`, `known_hosts` ni
una contraseña de Ubuntu. Las variables de correo de `backend/.env.example`
pertenecen al envío de correo y no intervienen en el almacenamiento de archivos.

Para ejecutar Java directamente, establece el perfil en la terminal antes de Maven:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'dev'
```

El archivo `backend/.env` contiene las propiedades de esa ejecución. Conserva el
perfil explícito de la terminal; no dependas de añadirlo solamente al archivo
importado. Variables reales del proceso pueden prevalecer sobre las de ese archivo.

## 6. Aplicar cambios sin reemplazar la configuración existente

1. Abre el `.example` apropiado y tu archivo local correspondiente.
2. Copia las variables nuevas y adapta sus valores. Conserva secretos y claves existentes.
3. Comprueba que la base, el host SFTP y la URL HTTP corresponden al entorno que usarás.
4. Valida el compose y aplica los cambios al servicio.

**Para Docker**, desde la raíz:

```powershell
docker compose config --quiet
docker compose up -d backend
```

`up` recrea el contenedor cuando cambia su configuración. Un simple `restart`
no vuelve a cargar variables de entorno modificadas en los archivos. Si también
cambió el código del backend, utiliza `docker compose up -d --build backend`.
Usa los mismos argumentos `-f` en validación y arranque si trabajas con complementos.

**Para Java nativo**, detén Maven y vuelve a iniciarlo. **Para Next.js**, reinicia
`npm run dev`; en producción, reconstruye y reinicia la aplicación para incorporar
el nuevo valor de `BACKEND_URL`.

Cambiar la IP de Ubuntu requiere mantener coherentes `ARCHIVOS_HOST`, la URL de
lectura, la identidad verificada de `known_hosts`, Netplan y el sitio de Nginx.
Reutiliza la clave privada ya autorizada si el backend sigue siendo el mismo.

El `docker-compose.prod.yml` tiene un servicio `app` y no incorpora por defecto
el montaje local de claves de archivos. Para habilitarlo en ese despliegue,
configura en `app` el montaje de claves de solo lectura y las rutas del contenedor,
además de las variables de almacenamiento. Los ejemplos de esta referencia son
para el compose de desarrollo; la configuración general de producción está en
[produccion.md](produccion.md).

Referencia: [interpolación y archivos de entorno en Docker Compose](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/).
