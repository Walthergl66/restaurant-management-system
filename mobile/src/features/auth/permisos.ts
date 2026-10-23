import type { UsuarioInfo } from './types';

/**
 * Política única de acceso a la atención de mesas; el backend valida cada
 * operación. Se resuelve por PERMISO (no por nombre de rol): cualquier
 * empleado con `pedidos:crear` puede atender, sea cual sea su rol.
 */
export function puedeAtenderMesas(usuario: UsuarioInfo | null): boolean {
  return !!usuario && !!usuario.permisos?.includes('pedidos:crear');
}

/**
 * Política de acceso a la sección de reparto (app del repartidor). Se resuelve
 * por PERMISO: solo el repartidor HABILITADO tiene `delivery:mis-entregas`
 * operativo (el backend separa el rol del estado operativo y valida todo). Un
 * DELIVERY no habilitado no llega aquí.
 */
export function puedeRepartir(usuario: UsuarioInfo | null): boolean {
  return !!usuario && !!usuario.permisos?.includes('delivery:mis-entregas');
}
