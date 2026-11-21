import { puedeAtenderMesas, puedeOperarCocina, puedeRepartir } from './permisos';
import type { UsuarioInfo } from './types';

/** Grupos de rutas de la app: uno por tipo de usuario. */
export type GrupoInicio = '/(delivery)' | '/(kitchen)' | '/(waiter)' | '/(tabs)';

/**
 * Pantalla de inicio que corresponde al usuario, resuelta por permiso (no por nombre de rol).
 * Prioridad: repartidor > mesero > cocina > cliente. El mesero también puede operar cocina (desde una
 * pestaña de su propia app), así que tener permisos de cocina no lo saca de su pantalla. El backend
 * valida cada operación igualmente.
 */
export function grupoInicio(usuario: UsuarioInfo | null): GrupoInicio {
  if (puedeRepartir(usuario)) return '/(delivery)';
  if (puedeAtenderMesas(usuario)) return '/(waiter)';
  if (puedeOperarCocina(usuario)) return '/(kitchen)';
  return '/(tabs)';
}
