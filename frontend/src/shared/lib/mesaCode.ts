/** Etiqueta visible de una mesa; el ID interno y el número de la API no cambian. */
export function mesaCode(numero: number): string {
  return `M${String(numero).padStart(2, '0')}`
}

export function siguienteNumeroMesa(numeros: number[]): number | null {
  const ocupados = new Set(numeros)
  for (let numero = 1; numero <= 99; numero++) {
    if (!ocupados.has(numero)) return numero
  }
  return null
}

/** Los pedidos guardan mesaId, que no es necesariamente el número visible. */
export function etiquetaMesa(mesaId: number, mesas: { id: number; numero: number }[]): string {
  const mesa = mesas.find(item => item.id === mesaId)
  return mesa ? `Mesa ${mesaCode(mesa.numero)}` : `Mesa (ID ${mesaId})`
}
