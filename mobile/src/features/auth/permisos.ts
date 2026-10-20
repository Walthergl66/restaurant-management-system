import type { UsuarioInfo } from './types';

/**
 * Política única de acceso a la atención de mesas; el backend valida cada
 * operación. Se resuelve por PERMISO (no por nombre de rol): cualquier
 * empleado con `pedidos:crear` puede atender, sea cual sea su rol.
 */
export function puedeAtenderMesas(usuario: UsuarioInfo | null): boolean {
  return !!usuario && !!usuario.permisos?.includes('pedidos:crear');
}
