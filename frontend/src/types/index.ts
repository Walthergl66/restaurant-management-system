export interface AuthResponse { accessToken: string; refreshToken: string }
export interface UserInfo { username: string; rol: string; permisos: string[] }

export type EstadoPedido = 'BORRADOR'|'CONFIRMADO'|'EN_PREPARACION'|'LISTO'|'ENTREGADO'|'CANCELADO'
export type EstadoMesa   = 'DISPONIBLE'|'OCUPADA'|'RESERVADA'|'FUERA_DE_SERVICIO'
export type EstadoCaja   = 'ABIERTA'|'CERRADA'
export type EstadoCuenta = 'ABIERTA'|'CERRADA'
export type EstadoAnulacion = 'SOLICITADA'|'APROBADA'|'RECHAZADA'
export type ComandaEstado   = 'PENDIENTE'|'EN_PREPARACION'|'LISTA'

export interface LineaPedido {
  id: number; productoId: number; productoNombre: string
  cantidad: number; precioUnitario: number; subtotal: number; anotaciones?: string
}
export interface PedidoResponse {
  id: number; codigo: string; mesaId?: number; mesaNombre?: string
  estado: EstadoPedido; total: number; creadoAt: string; actualizadoAt: string
  lineas: LineaPedido[]; creadoPor?: string
}
export interface Mesa { id: number; nombre: string; capacidad: number; estado: EstadoMesa; zona?: string }
export interface Categoria { id: number; nombre: string; descripcion?: string; activa: boolean }
export interface Producto {
  id: number; nombre: string; descripcion?: string; precio: number
  categoriaId: number; categoriaNombre?: string; activo: boolean; disponible: boolean
}
export interface CajaResponse {
  id: number; estado: EstadoCaja; aperturaInicial: number
  cierreEsperado?: number; cierreReal?: number; diferencia?: number
  abiertaPor?: string; cerradaPor?: string; abiertaAt: string; cerradaAt?: string
}
export interface MovimientoResponse {
  id: number; tipo: 'INGRESO'|'EGRESO'; concepto: string; monto: number; metodo?: string; fecha: string
}
export interface AnulacionResponse {
  id: number; pedidoCodigo: string; lineaId: number; nombreProducto: string
  precioUnitario: number; cantidad: number; motivo?: string; estado: EstadoAnulacion
  solicitadoPor: string; resueltoPor?: string; resueltaAt?: string; creadoAt: string
}
export interface CuentaResponse {
  id: number; mesaId: number; mesaNombre: string; estado: EstadoCuenta; total: number; creadoAt: string
}
export interface UsuarioAdminResponse {
  id: number; username: string; nombre: string; apellido: string
  email: string; rol: string; activo: boolean; creadoAt: string
}
export interface RolDto { id: number; nombre: string; permisos: string[] }
export interface EventoAuditoria {
  id: number; usuario: string; tipo: string; entidad: string
  entidadId?: string; detalle?: string; fecha: string
}
export interface ReporteVentas {
  totalVentas: number; totalPedidos: number; ticketPromedio: number
  porMetodo: { metodo: string; monto: number }[]
  porDia: { fecha: string; monto: number; pedidos: number }[]
  topProductos: { productoNombre: string; cantidad: number; monto: number }[]
}
export interface ResumenFinanzas { ingresos: number; egresos: number; resultado: number }
export interface ComandaResponse {
  id: number; pedidoCodigo: string; areaNombre: string; estado: ComandaEstado
  items: { productoNombre: string; cantidad: number; anotaciones?: string }[]
  creadoAt: string
}
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
