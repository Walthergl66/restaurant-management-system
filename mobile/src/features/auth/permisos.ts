import type { UsuarioInfo } from './types';

/** Política única de acceso a la atención de mesas; el backend valida cada operación. */
export function puedeAtenderMesas(usuario: UsuarioInfo | null): boolean {
  return !!usuario && (usuario.rol === 'MESERO' || !!usuario.permisos?.includes('pedidos:crear'));
}
