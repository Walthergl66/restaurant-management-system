#!/usr/bin/env bash
# Ejecutar en la VM: sudo bash preparar-archivos-ubuntu.sh archivos_backend.pub
set -euo pipefail
[[ $EUID -eq 0 ]] || { echo 'Ejecuta con sudo.'; exit 1; }
PUB=${1:?Indica el archivo de clave PUBLICA del backend}
[[ -f "$PUB" ]] && grep -Eq '^ssh-ed25519 [A-Za-z0-9+/=]+( .*)?$' "$PUB" || { echo 'Clave pública inválida'; exit 1; }
BACKUP="/root/antes-archivos-$(date +%Y%m%d-%H%M%S).tar.gz"
tar -czf "$BACKUP" /etc/nginx /srv/restaurante/media
echo "Respaldo de Nginx y archivos: $BACKUP"
apt-get update
apt-get install -y acl openssh-server
id archivosapp >/dev/null 2>&1 || useradd --create-home --shell /usr/sbin/nologin archivosapp
install -d -o archivosapp -g archivosapp -m 700 /home/archivosapp/.ssh
# Restricción por clave: solo SFTP, sin shell, terminal ni reenvíos.
KEY="restrict,command=\"internal-sftp -d /srv/restaurante/media\" $(cat "$PUB")"
touch /home/archivosapp/.ssh/authorized_keys
grep -Fqx "$KEY" /home/archivosapp/.ssh/authorized_keys || printf '%s\n' "$KEY" >> /home/archivosapp/.ssh/authorized_keys
chown archivosapp:archivosapp /home/archivosapp/.ssh/authorized_keys
chmod 600 /home/archivosapp/.ssh/authorized_keys
for folder in /srv/restaurante/media /srv/restaurante/media/imagenes /srv/restaurante/media/videos; do
    [[ -d "$folder" ]] || { echo "Falta carpeta: $folder"; exit 1; }
    setfacl -m u:archivosapp:rwx "$folder"
done
systemctl enable --now ssh
echo 'Listo: acceso SFTP dedicado preparado. No se modificaron los archivos existentes.'
