/** Conserva marcas, siglas y tildes. Las descripciones pueden contener saltos de línea. */
export function normalizarTextoCatalogo(texto: string, tipo: 'nombre' | 'descripcion' = 'nombre'): string {
  const espacios = tipo === 'descripcion' ? /[^\S\r\n]+/gu : /\s+/gu
  const limpio = texto.replace(espacios, ' ').trim()
  const primera = Array.from(limpio)[0]
  return primera ? primera.toLocaleUpperCase('es') + limpio.slice(primera.length) : ''
}
