export interface Categoria {
  id: number; nombre: string; descripcion?: string | null; orden: number; activo: boolean
}

export interface Extra {
  id: number; nombre: string; descripcion?: string | null; precio: number; activo: boolean
}

export interface Producto {
  id: number; nombre: string; descripcion?: string | null; imagenUrl?: string | null; precio: number
  categoriaId?: number | null; categoriaNombre?: string | null
  areaId?: number | null; areaNombre?: string | null
  activo: boolean; extras: Extra[]; ingredientes: string[]
}

export interface ProductoPayload {
  nombre: string
  descripcion: string | null
  imagenUrl: string | null
  precio: number
  categoriaId: number | null
  areaId: number | null
  extraIds: number[]
  ingredientes: string[]
}
