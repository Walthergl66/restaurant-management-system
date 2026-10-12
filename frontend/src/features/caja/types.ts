export type EstadoCaja = 'ABIERTA'|'CERRADA'

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
