/** Conserva las mayúsculas internas de marcas y siglas. */
export function normalizarNombreCatalogo(nombre: string): string {
  const limpio = nombre.replace(/\s+/gu, ' ').trim()
  const primera = Array.from(limpio)[0]
  return primera ? primera.toLocaleUpperCase('es') + limpio.slice(primera.length) : ''
}
