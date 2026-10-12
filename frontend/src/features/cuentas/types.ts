export type EstadoCuenta = 'ABIERTA'|'CERRADA'

export interface CuentaLinea {
  lineaId: number; productoId: number; nombreProducto: string
  cantidad: number; precioUnitario: number; subtotal: number
}

export interface CuentaPedido { codigo: string; estado: string; subtotal: number; lineas: CuentaLinea[] }

export interface CuentaAnulacion {
  id: number; pedidoCodigo: string; lineaId: number; nombreProducto: string
  precioUnitario: number; cantidad: number; motivo?: string | null
}

export interface CuentaResponse {
  id: number; mesaId: number; numeroMesa: number; estado: EstadoCuenta; version?: number | null
  total: number; pedidos: CuentaPedido[]; anulaciones: CuentaAnulacion[]
  createdAt: string; updatedAt: string
}

export interface PagoPayload {
  metodo: string
  monto: number
}

export interface CobroPayload {
  pagos: PagoPayload[]
  documento: unknown | null
}

export interface AdicionPayload {
  codigo: string
  notas: string | null
}
