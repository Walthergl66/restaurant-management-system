export type EstadoMesa = 'LIBRE' | 'OCUPADA' | 'RESERVADA' | 'INACTIVA'

export interface Mesa {
  id: number; numero: number; capacidad: number
  /** Nombre de la zona (se conserva por compatibilidad con el backend). */
  ubicacion?: string | null
  zonaId: number; zonaOrden: number
  estado: EstadoMesa; activo: boolean
}

export interface MesaPayload {
  numero: number
  capacidad: number
  /** Si falta, el backend usa la primera zona (al crear) o conserva la actual (al editar). */
  zonaId?: number
}

/** Zona del restaurante (Salón, Terraza…) con el número de mesas que contiene. */
export interface Zona {
  id: number
  nombre: string
  orden: number
  cantidadMesas: number
}
