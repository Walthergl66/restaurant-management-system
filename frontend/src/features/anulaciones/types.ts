export type EstadoAnulacion = 'SOLICITADA'|'APROBADA'|'RECHAZADA'

export interface AnulacionResponse {
  id: number; pedidoCodigo: string; lineaId: number; productoId: number; nombreProducto: string
  precioUnitario: number; cantidad: number; motivo?: string | null; estado: EstadoAnulacion
  solicitadoPor: string; resueltoPor?: string | null; resueltaAt?: string | null
}
