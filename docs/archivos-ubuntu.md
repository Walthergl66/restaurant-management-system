# Imágenes y videos en Ubuntu

## Recorrido

El formulario de productos acepta una imagen principal (JPG/PNG/WebP, 10 MiB) y
un video opcional (MP4/WebM, 100 MiB). La vista previa es local; la transferencia
empieza al guardar. El backend detecta el tipo por contenido, publica por SFTP
con nombre UUID y renombrado de `.part`, y registra metadatos en PostgreSQL.
La URL estable `/api/v1/archivos/{uuid}/contenido` consulta Nginx desde el backend:
el navegador y el móvil no necesitan acceder directamente a la IP de Ubuntu.

La API permite GET/HEAD públicos solo para archivos vinculados a productos.
Los temporales son visibles únicamente para quien los subió. Subir y descartar
requiere `catalogo:gestionar`; descartar solo elimina temporales propios.
El video se transmite por partes; se admiten rangos HTTP simples, sufijos y HEAD.
No hay conversión de códecs: usar MP4 H.264/AAC o WebM compatible con el reproductor.
En móvil, «Ver video del producto» abre el reproductor/navegador del dispositivo.

## Preparación de la VM (una vez)

Se presupone Ubuntu accesible en `192.168.100.77`, Nginx con `/media/` apuntando a
`/srv/restaurante/media/`, carpetas `imagenes` y `videos`, y usuario `oscar` con sudo.

1. Crear clave del backend fuera del control de versiones:

   ```powershell
   New-Item -ItemType Directory -Force backend/.local/ssh
   ssh-keygen -t ed25519 -f backend/.local/ssh/archivos_backend
   ```

   Para uso automático sin agente, guardar la clave sin frase de paso y proteger
   su acceso en el host. Nunca subir la clave privada al repositorio ni a Ubuntu.
   Si la clave ya existe, conservarla: no sobrescribirla.

2. Verificar la identidad del servidor desde su consola:

   ```bash
   sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub
   ```

   El `known_hosts` del backend debe contener esa identidad verificada. No usar
   verificadores permisivos ni desactivar StrictHostKeyChecking. Para reutilizar
   una entrada ya verificada desde Windows:

   ```powershell
   ssh-keygen -F 192.168.100.77 -f "$env:USERPROFILE\.ssh\known_hosts" | Set-Content -Encoding ascii backend/.local/ssh/known_hosts
   ssh-keygen -lf backend/.local/ssh/known_hosts
   ```

3. Desde la raíz del repositorio en PowerShell:

   ```powershell
   scp .\scripts\preparar-archivos-ubuntu.sh .\backend\.local\ssh\archivos_backend.pub oscar@192.168.100.77:/home/oscar/
   ```

4. Desde la consola de Ubuntu:

   ```bash
   sudo bash /home/oscar/preparar-archivos-ubuntu.sh /home/oscar/archivos_backend.pub
   ```

   El script respalda Nginx y media en `/root/antes-archivos-*.tar.gz`, instala ACL
   y SSH, crea `archivosapp` y autoriza solo SFTP para esa clave. Usa ACL para
   permitir escritura en las tres carpetas sin sustituir al propietario `oscar`.
   Es repetible. No requiere cambiar el router. Nginx necesita lectura y recorrido
   mediante el grupo `www-data`; los archivos publicados tienen modo `640`.

Si APT informa que InRelease «no es válido todavía», corregir el reloj de la VM.
Activar NTP con `sudo timedatectl set-ntp true` y comprobar `timedatectl status`.
Si no sincroniza, usar la hora UTC actual del host para un ajuste manual temporal;
después reactivar NTP. No desactivar las comprobaciones de fecha o firma de APT.

## Ejecutar

Con Docker desde la raíz (montaje de claves de solo lectura):

```powershell
docker compose -f docker-compose.yml -f docker-compose.archivos.yml up -d --build backend
```

También puede usarse el compose habitual guardando en el `.env` de la raíz
`DB_NAME=restaurante_archivos` y `ARCHIVOS_HABILITADO=true`. El compose base monta
las claves y mantiene las subidas desactivadas por defecto para instalaciones
que todavía no tengan la VM preparada. Se configuró así el entorno local.

La migración V12 crea el esquema `archivos`, añade referencias opcionales a los
productos y un campo de versión para detectar cambios simultáneos. No cambia las
migraciones anteriores ni elimina imágenes existentes.

El complemento local usa `restaurante_archivos` como base de desarrollo
(`ARCHIVOS_DB_NAME` permite cambiarla). Crear esa base previamente en PostgreSQL.
Se separó de la base local antigua porque aquella tenía un historial V9–V11
de otra versión del proyecto. No ejecutar `repair` para ocultar diferencias de
esquema. La base anterior `restaurante` y su respaldo se conservan.

Sin Docker, configurar en `backend/.env`:

```dotenv
ARCHIVOS_HABILITADO=true
ARCHIVOS_HOST=192.168.100.77
ARCHIVOS_USUARIO=archivosapp
ARCHIVOS_CLAVE_PRIVADA=C:/ruta/al/proyecto/backend/.local/ssh/archivos_backend
ARCHIVOS_KNOWN_HOSTS=C:/ruta/al/proyecto/backend/.local/ssh/known_hosts
ARCHIVOS_LECTURA_URL=http://192.168.100.77/media/
```

Sin configuración, el resto del sistema arranca normalmente y las subidas
responden 503 con un mensaje de almacenamiento no disponible.
La clave y known_hosts deben ser legibles por el usuario del proceso Java.
No usar los paths de Windows dentro del contenedor.

## Persistencia y fallos

- La vinculación del archivo y la modificación del producto comparten transacción
  de PostgreSQL. La imagen anterior se conserva si guardar el producto falla.
- Un cliente anterior que solo envía `imagenUrl` puede conservar imágenes
  gestionadas. Los campos de video son opcionales. Para quitar video se envía
  `quitarVideo: true`; omitirlo conserva el existente.
- Cancelar el formulario intenta descartar subidas temporales de intentos previos.
  Cada 10 minutos se limpian hasta 50 temporales o retirados de más de 24 horas,
  bloqueando cada registro para coordinar con una vinculación concurrente.
- Si PostgreSQL revierte la subida, se intenta eliminar el archivo remoto. Si el
  servidor está caído durante esa compensación, se registra la clave UUID para
  conciliación manual. Un corte abrupto del proceso también puede dejar `.part`:
  reconciliar respaldando y comparando con los registros, nunca borrar por nombre
  original. La limpieza automática no recorre ni elimina archivos ajenos al módulo.
- Nginx sigue publicando el directorio local: no almacenar datos privados allí.
  Un archivo retirado deja de ser accesible por el endpoint del backend; una URL
  directa de Nginx puede seguir funcionando hasta que la limpieza lo elimine.
- Respaldar PostgreSQL y el directorio media en otro disco/equipo, de forma
  coordinada. Una instantánea en el mismo disco no es un respaldo independiente.

## Comprobaciones

1. Subir una imagen, guardar y recargar el catálogo.
2. Reiniciar backend y comprobar que se conserva.
3. Reemplazarla; cancelar otra selección y verificar que no cambia el producto.
4. Subir MP4/WebM, reproducir y avanzar dentro del video.
5. Comprobar rechazos por formato, tamaño y falta de permiso.
6. Apagar temporalmente la VM y comprobar un error legible, sin perder el medio anterior.

Pruebas automatizadas en backend: `ArchivosIntegrationTest`, `ValidadorArchivoTest`
y `ProductoRequestCompatibilidadTest`. La integración usa PostgreSQL real de
Testcontainers, almacenamiento simulado y un servidor HTTP local para verificar
los rangos y la transmisión sin depender de la disponibilidad de la VM.

En este Windows, Java 25 falla al crear selectores con sockets en la ruta TEMP
abreviada del usuario. Para ejecutar las pruebas de WebSocket/correo existentes
se verificó un directorio temporal sin esa abreviatura:

```powershell
New-Item -ItemType Directory -Force C:/Users/Public/rms-sockets
mvn.cmd test '-DargLine=-Djdk.net.unixdomain.tmpdir=C:/Users/Public/rms-sockets'
```

La imagen de prueba del navegador se generó localmente; el MP4 de validación
es el ejemplo CC0 de MDN (`interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4`).
Ningún archivo del restaurante se almacena en ese servicio: las subidas se
realizan exclusivamente a la VM local.
