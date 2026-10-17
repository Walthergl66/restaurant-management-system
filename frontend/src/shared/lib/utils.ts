import type { AxiosError } from 'axios'
import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function fmt(n?: number | null) {
  return `$${(n ?? 0).toLocaleString('es-AR', { minimumFractionDigits: 2 })}`
}

export function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return 'Buenos días'
  if (h < 19) return 'Buenas tardes'
  return 'Buenas noches'
}

export function getTurno() {
  const h = new Date().getHours()
  if (h >= 6 && h < 14) return 'Turno mañana'
  if (h >= 14 && h < 22) return 'Turno tarde'
  return 'Turno nocturno'
}

/**
 * Extrae el mensaje legible de un error del backend (ProblemDetail RFC 7807).
 * El backend responde `{ title, detail, fieldErrors? }`; los errores de
 * validación traen los detalles en `fieldErrors`.
 */
export function errMsg(e: unknown, fallback = 'Ocurrió un error'): string {
  const data = (e as AxiosError<{
    detail?: string
    title?: string
    fieldErrors?: Record<string, string>
  }>)?.response?.data
  if (!data) return fallback
  const fieldErrors = data.fieldErrors
  if (fieldErrors && Object.keys(fieldErrors).length > 0) {
    return Object.values(fieldErrors).join('. ')
  }
  return data.detail || data.title || fallback
}

export const ESTADO_PEDIDO: Record<string, { label: string; badge: string }> = {
  BORRADOR:        { label: 'Nuevo',      badge: 'badge-nuevo' },
  CONFIRMADO:      { label: 'Confirmado', badge: 'badge-confirmado' },
  EN_PREPARACION:  { label: 'Preparando', badge: 'badge-preparacion' },
  LISTO:           { label: 'Listo',      badge: 'badge-listo' },
  ENTREGADO:       { label: 'Entregado',  badge: 'badge-entregado' },
  ANULADO:         { label: 'Anulado',    badge: 'badge-cancelado' },
}

export const ESTADO_MESA: Record<string, { label: string; badge: string }> = {
  LIBRE:     { label: 'Libre',     badge: 'badge-listo' },
  OCUPADA:   { label: 'Ocupada',   badge: 'badge-preparacion' },
  RESERVADA: { label: 'Reservada', badge: 'badge-confirmado' },
  INACTIVA:  { label: 'Inactiva',  badge: 'badge-cancelado' },
}

export const METODOS_PAGO: { value: string; label: string }[] = [
  { value: 'EFECTIVO', label: 'Efectivo' },
  { value: 'TARJETA', label: 'Tarjeta' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
  { value: 'OTRO', label: 'Otro' },
]
