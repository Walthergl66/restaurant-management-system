export type EstadoPedido = 'BORRADOR'|'CONFIRMADO'|'EN_PREPARACION'|'LISTO'|'ENTREGADO'|'ANULADO'

export interface ExtraLinea { extraId: number; nombre: string; precio: number }

export interface LineaPedido {
  id: number; productoId: number; nombreProducto: string
  cantidad: number; precioUnitario: number; subtotal: number
  observaciones?: string | null
  extras: ExtraLinea[]
  ingredientesRemovidos: string[]
}

export interface PedidoResponse {
  codigo: string; mesaId?: number | null; estado: EstadoPedido; notas?: string | null
  total: number; lineas: LineaPedido[]; createdAt: string; updatedAt: string
}
