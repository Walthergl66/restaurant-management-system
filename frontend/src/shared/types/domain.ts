export type MetodoPago      = 'EFECTIVO'|'TARJETA'|'TRANSFERENCIA'|'OTRO'

export type { Mesa, EstadoMesa } from '@/features/mesas/types'
export type { Categoria, Extra, Producto } from '@/features/catalogo/types'
export type { EstadoPedido, ExtraLinea, LineaPedido, PedidoResponse } from '@/features/pedidos/types'
export type { ComandaEstado, ComandaLinea, ComandaResponse } from '@/features/comandas/types'
export type { EstadoCuenta, CuentaLinea, CuentaPedido, CuentaAnulacion, CuentaResponse } from '@/features/cuentas/types'
export type { EstadoCaja, CajaResponse, MovimientoResponse } from '@/features/caja/types'
export type { EstadoAnulacion, AnulacionResponse } from '@/features/anulaciones/types'

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
