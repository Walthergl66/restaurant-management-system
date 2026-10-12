export type EstadoPedido = 'BORRADOR'|'CONFIRMADO'|'EN_PREPARACION'|'LISTO'|'ENTREGADO'|'ANULADO'
export type EstadoMesa   = 'LIBRE'|'OCUPADA'|'RESERVADA'|'INACTIVA'
export type EstadoCaja   = 'ABIERTA'|'CERRADA'
export type EstadoCuenta = 'ABIERTA'|'CERRADA'
export type EstadoAnulacion = 'SOLICITADA'|'APROBADA'|'RECHAZADA'
export type ComandaEstado   = 'PENDIENTE'|'EN_PREPARACION'|'LISTO'
export type MetodoPago      = 'EFECTIVO'|'TARJETA'|'TRANSFERENCIA'|'OTRO'

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
export interface Mesa {
  id: number; numero: number; capacidad: number; ubicacion?: string | null
  estado: EstadoMesa; activo: boolean
}
export interface Categoria { id: number; nombre: string; descripcion?: string | null; orden: number; activo: boolean }
export interface Extra { id: number; nombre: string; descripcion?: string | null; precio: number; activo: boolean }
export interface Producto {
  id: number; nombre: string; descripcion?: string | null; imagenUrl?: string | null; precio: number
  categoriaId?: number | null; categoriaNombre?: string | null
  areaId?: number | null; areaNombre?: string | null
  activo: boolean; extras: Extra[]; ingredientes: string[]
}
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
export interface ComandaLinea {
  productoId: number; nombreProducto: string; cantidad: number
  extras?: string | null; ingredientes?: string | null; observaciones?: string | null; orden: number
}
export interface ComandaResponse {
  id: number; pedidoCodigo: string; numeroComanda: number; areaId?: number | null; areaNombre: string
  tipo: string; estado: ComandaEstado; lineas: ComandaLinea[]
}
