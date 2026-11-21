import { puedeAtenderMesas, puedeOperarCocina, puedeRepartir } from './permisos';
import type { UsuarioInfo } from './types';

/** Grupos de rutas de la app: uno por tipo de usuario. */
export type GrupoInicio = '/(delivery)' | '/(kitchen)' | '/(waiter)' | '/(tabs)';

/**
 * Pantalla de inicio que corresponde al usuario, resuelta por permiso (no por nombre de rol).
 * Prioridad: repartidor > cocina > mesero > cliente. El backend valida cada operación igualmente.
 */
export function grupoInicio(usuario: UsuarioInfo | null): GrupoInicio {
  if (puedeRepartir(usuario)) return '/(delivery)';
  if (puedeOperarCocina(usuario)) return '/(kitchen)';
  if (puedeAtenderMesas(usuario)) return '/(waiter)';
  return '/(tabs)';
}
