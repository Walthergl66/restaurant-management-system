export type EstadoRepartidor = 'PENDIENTE' | 'HABILITADO' | 'SUSPENDIDO'

export type EstadoAsignacion =
  | 'ASIGNADA'
  | 'EN_RUTA'
  | 'ENTREGADA'
  | 'FALLIDA'
  | 'REASIGNADA'

/** Espejo de DeliveryAdminController.RepartidorResponse */
export interface RepartidorResponse {
  id: number
  usuarioId: number
  estado: EstadoRepartidor
  habilitadoPor?: string | null
  habilitadoAt?: string | null
}

/** Espejo de DeliveryAsignacionesController.PedidoListoResponse */
export interface PedidoListoResponse {
  pedidoClienteId: number
  codigo?: string | null
  clienteNombre?: string | null
  direccion?: string | null
  telefono?: string | null
}

/** Espejo de ConfigRestauranteResumen (config-restaurante). lat/lng llegan como número (BigDecimal). */
export interface ConfigRestauranteResumen {
  direccion: string | null
  lat: number | null
  lng: number | null
  origenCoords: 'GEOCODING' | 'MANUAL' | null
  tieneCoordenadas: boolean
  requiereSeleccionManual: boolean
}

/** Espejo de DeliveryConfirmacionesController.ConfirmacionPendienteResponse */
export interface ConfirmacionPendienteResponse {
  paradaId: number
  asignacionId: number
  pedidoClienteId: number
  codigo?: string | null
  direccion?: string | null
  estado?: string | null
  faseConfirmacion?: string | null
  repartidorConfirmoAt?: string | null
  clienteConfirmoAt?: string | null
  entregaVerificada: boolean
}

/** Espejo de DeliveryAsignacionesController.AsignacionResponse */
export interface AsignacionResponse {
  asignacionId: number
  pedidoClienteId: number
  repartidorId: number
  estado: EstadoAsignacion
  asignadoPor?: string | null
  asignadoAt?: string | null
  codigo?: string | null
  clienteNombre?: string | null
  direccion?: string | null
  telefono?: string | null
}
