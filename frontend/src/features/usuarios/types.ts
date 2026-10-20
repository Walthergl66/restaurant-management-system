export interface UsuarioAdminResponse { id: number; username: string; nombre: string; activo: boolean; rol: string; permisos: string[] }

export interface RolDto { id: number; codigo: string; descripcion: string; permisos: string[] }

export interface PermisoDto { codigo: string; modulo: string; descripcion: string }

export interface CrearUsuarioPayload {
  username: string
  nombre: string
  password: string
  rolCodigo: string
  permisos?: string[]
}

export interface ActualizarUsuarioPayload {
  nombre: string
  rolCodigo: string
  activo: boolean
  permisos?: string[]
}
