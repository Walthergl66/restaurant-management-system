/** Lógica pura del perfil de usuario (sin React Native, para poder probarla con node --test). */

const ETIQUETAS_ROL: Record<string, string> = {
  ADMIN: 'Administrador',
  MESERO: 'Mesero',
  CAJERO: 'Cajero',
  COCINA: 'Cocina',
  DELIVERY: 'Repartidor',
  CLIENTE: 'Cliente',
};

/** Iniciales (máximo dos) para el avatar; "?" si no hay nombre. */
export function iniciales(nombre: string | null | undefined): string {
  const palabras = (nombre ?? '').trim().split(/\s+/).filter(Boolean);
  if (palabras.length === 0) return '?';
  return palabras.slice(0, 2).map(palabra => palabra[0].toUpperCase()).join('');
}

/** Nombre legible del rol; un rol desconocido se muestra tal cual. */
export function etiquetaRol(rol: string | null | undefined): string {
  if (!rol) return '';
  return ETIQUETAS_ROL[rol] ?? rol;
}
