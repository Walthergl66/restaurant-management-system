/**
 * Quién puede usar la aplicación web. La web es para la administración y la caja; el mesero, la
 * cocina, el repartidor y el cliente trabajan desde la aplicación móvil.
 *
 * Es un candado de la interfaz: la API sigue validando los permisos de cada operación.
 */

export const ROLES_WEB = ['ADMIN', 'CAJERO']

export const MENSAJE_SOLO_MOVIL = 'Esta cuenta usa la aplicación móvil. Ingresa desde la app.'

/** Error que se lanza al iniciar sesión con una cuenta que no puede usar la web. */
export class SoloMovilError extends Error {
  constructor() {
    super(MENSAJE_SOLO_MOVIL)
    this.name = 'SoloMovilError'
  }
}

export function puedeUsarWeb(user: { rol?: string | null } | null | undefined): boolean {
  return !!user?.rol && ROLES_WEB.includes(user.rol)
}
