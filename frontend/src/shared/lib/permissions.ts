export interface AuthIdentity {
  rol?: string | null
  permisos?: string[] | null
}

/* Roles con acceso total: ven y hacen todo, sin importar la lista de permisos. */
export const ROLES_ACCESO_TOTAL = ['ADMIN']

export function hasPermiso(user: AuthIdentity | null | undefined, ...codigos: string[]) {
  if (!user) return false
  if (user.rol && ROLES_ACCESO_TOTAL.includes(user.rol)) return true
  return codigos.some((c) => user.permisos?.includes(c))
}
