export type EstadoMesa = 'LIBRE' | 'OCUPADA' | 'RESERVADA' | 'INACTIVA'

export interface Mesa {
  id: number; numero: number; capacidad: number; ubicacion?: string | null
  estado: EstadoMesa; activo: boolean
}

export interface MesaPayload {
  numero: number
  capacidad: number
  ubicacion: string | null
}
