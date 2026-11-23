import type { Mesa, Zona } from './types'

/** Devuelve los ids con la zona de `indice` movida una posición (-1 sube, 1 baja); sin cambios en los extremos. */
export function moverZona(ids: number[], indice: number, delta: -1 | 1): number[] {
  const destino = indice + delta
  if (indice < 0 || indice >= ids.length || destino < 0 || destino >= ids.length) return [...ids]
  const copia = [...ids]
  ;[copia[indice], copia[destino]] = [copia[destino], copia[indice]]
  return copia
}

export interface GrupoZona {
  zona: Zona
  mesas: Mesa[]
}

/** Agrupa las mesas por zona en el orden de las zonas (las vacías incluidas); las huérfanas van al final. */
export function agruparMesasPorZona(zonas: Zona[], mesas: Mesa[]): GrupoZona[] {
  const ordenadas = [...zonas].sort((a, b) => a.orden - b.orden || a.id - b.id)
  const conocidas = new Set(ordenadas.map(z => z.id))
  const porNumero = (a: Mesa, b: Mesa) => a.numero - b.numero

  const grupos: GrupoZona[] = ordenadas.map(zona => ({
    zona,
    mesas: mesas.filter(m => m.zonaId === zona.id).sort(porNumero),
  }))

  const huerfanas = mesas.filter(m => !conocidas.has(m.zonaId)).sort(porNumero)
  if (huerfanas.length > 0) {
    grupos.push({
      zona: { id: 0, nombre: 'Sin zona', orden: Number.MAX_SAFE_INTEGER, cantidadMesas: huerfanas.length },
      mesas: huerfanas,
    })
  }
  return grupos
}
