export interface AuthIdentity {
  rol?: string | null
  permisos?: string[] | null
}

export function hasPermiso(user: AuthIdentity | null | undefined, ...codigos: string[]) {
  if (!user) return false
  if (user.rol === 'ADMIN') return true
  return codigos.some((c) => user.permisos?.includes(c))
}
