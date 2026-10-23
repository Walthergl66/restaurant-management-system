export interface UsuarioAdminResponse { id: number; username: string; nombre: string; celular?: string | null; activo: boolean; rol: string; permisos: string[] }

/** Rol con sus permisos por defecto y las banderas que decide el panel. */
export interface RolDto {
  id: number
  codigo: string
  descripcion: string
  /** Categoría a la que pertenece el rol (ADMIN, EMPLEADO, CLIENTE, DELIVERY). */
  categoria: string
  categoriaDescripcion: string
  /** Si el administrador puede crear usuarios con este rol desde el panel. */
  asignablePorAdmin: boolean
  /** Si el administrador puede ajustar las funcionalidades de estos usuarios. */
  configurable: boolean
  /** De dónde nace la cuenta con este rol: ADMIN o AUTO_REGISTRO. */
  origen: string
  permisos: string[]
}

/** Categoría de roles con los roles que contiene. */
export interface CategoriaRolDto {
  codigo: string
  descripcion: string
  roles: RolDto[]
}

/** Taxonomía de roles: las categorías con sus roles y la lista plana. */
export interface TaxonomiaRolesDto {
  categorias: CategoriaRolDto[]
  roles: RolDto[]
}

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
