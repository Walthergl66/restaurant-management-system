export type EstadoCaja   = 'ABIERTA'|'CERRADA'
export type EstadoCuenta = 'ABIERTA'|'CERRADA'
export type EstadoAnulacion = 'SOLICITADA'|'APROBADA'|'RECHAZADA'
export type MetodoPago      = 'EFECTIVO'|'TARJETA'|'TRANSFERENCIA'|'OTRO'

export type { Mesa, EstadoMesa } from '@/features/mesas/types'
export type { Categoria, Extra, Producto } from '@/features/catalogo/types'
export type { EstadoPedido, ExtraLinea, LineaPedido, PedidoResponse } from '@/features/pedidos/types'
export type { ComandaEstado, ComandaLinea, ComandaResponse } from '@/features/comandas/types'

export interface CajaResponse {
  id: number; estado: EstadoCaja; aperturaInicial: number
  cierreEsperado?: number | null; cierreReal?: number | null; diferencia?: number | null
  version?: number | null; abiertaPor?: string | null; cerradaPor?: string | null
  abiertaAt: string; cerradaAt?: string | null
}
export interface MovimientoResponse {
  id: number; cajaId: number; tipo: 'INGRESO'|'EGRESO'; concepto: string
  monto: number; metodo?: string | null; pagoId?: number | null; created_at: string
}
export interface AnulacionResponse {
  id: number; pedidoCodigo: string; lineaId: number; productoId: number; nombreProducto: string
  precioUnitario: number; cantidad: number; motivo?: string | null; estado: EstadoAnulacion
  solicitadoPor: string; resueltoPor?: string | null; resueltaAt?: string | null
}
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
export interface UsuarioAdminResponse { id: number; username: string; nombre: string; activo: boolean; rol: string }
export interface RolDto { id: number; codigo: string; descripcion: string; permisos: string[] }
export interface EventoAuditoria {
  id: number; usuario: string; tipo: string; entidad: string
  entidadId?: string | null; detalle?: string | null; fecha: string
}
export interface ReporteVentas {
  total: number; numeroVentas: number
  porMetodo: { metodo: string; total: number }[]
  porDia: { fecha: string; total: number }[]
  porProducto: { productoId: string; nombre: string; cantidad: number; monto: number }[]
}
export interface ResumenFinanzas { ingresos: number; egresos: number; resultado: number }
