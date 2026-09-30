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

export const ESTADO_PEDIDO: Record<string, { label: string; badge: string }> = {
  BORRADOR:        { label: 'Nuevo',      badge: 'badge-nuevo' },
  CONFIRMADO:      { label: 'Confirmado', badge: 'badge-confirmado' },
  EN_PREPARACION:  { label: 'Preparando', badge: 'badge-preparacion' },
  LISTO:           { label: 'Listo',      badge: 'badge-listo' },
  ENTREGADO:       { label: 'Entregado',  badge: 'badge-entregado' },
  CANCELADO:       { label: 'Cancelado',  badge: 'badge-cancelado' },
}
