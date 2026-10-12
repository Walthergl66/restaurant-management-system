export type ComandaEstado = 'PENDIENTE'|'EN_PREPARACION'|'LISTO'

export interface ComandaLinea {
  productoId: number; nombreProducto: string; cantidad: number
  extras?: string | null; ingredientes?: string | null; observaciones?: string | null; orden: number
}

export interface ComandaResponse {
  id: number; pedidoCodigo: string; numeroComanda: number; areaId?: number | null; areaNombre: string
  tipo: string; estado: ComandaEstado; lineas: ComandaLinea[]
}
