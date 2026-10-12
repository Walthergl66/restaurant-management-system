'use client'
import { useId, useRef, useState } from 'react'
import type { PointerEvent } from 'react'
import { GripVertical } from 'lucide-react'
import { useOrdenCategorias } from '../hooks'
import type { Categoria } from '../types'

export function useReordenCategorias(categorias: Categoria[]) {
  const mutation = useOrdenCategorias()
  const instruccionesId = useId()
  const lista = useRef<HTMLTableSectionElement>(null)
  const arrastre = useRef<{ id: number; ids: number[] } | null>(null)
  const [vistaPrevia, setVistaPrevia] = useState<number[] | null>(null)
  const [arrastrando, setArrastrando] = useState<number | null>(null)
  const [anuncio, setAnuncio] = useState('')
  const porId = new Map(categorias.map(c => [c.id, c]))
  const visibles = vistaPrevia?.map(id => porId.get(id)).filter((c): c is Categoria => !!c) ?? categorias
  const guardar = (ids: number[]) => {
    setVistaPrevia(ids)
    mutation.mutate({ modo: 'PERSONALIZADO', categoriaIds: ids }, {
      onSettled: () => setVistaPrevia(null),
    })
  }
  const iniciar = (event: PointerEvent<HTMLButtonElement>, id: number) => {
    if (event.button !== 0 || mutation.isPending) return
    event.preventDefault()
    event.currentTarget.focus()
    event.currentTarget.setPointerCapture(event.pointerId)
    arrastre.current = { id, ids: categorias.map(c => c.id) }
    setArrastrando(id)
  }
  const arrastrar = (event: PointerEvent<HTMLButtonElement>) => {
    const actual = arrastre.current
    if (!actual) return
    const filas = Array.from(lista.current?.querySelectorAll<HTMLElement>('[data-categoria-id]') ?? [])
    const destino = filas.find(fila => {
      const bounds = fila.getBoundingClientRect()
      return event.clientY >= bounds.top && event.clientY <= bounds.bottom
    })
    if (!destino) return
    const desde = actual.ids.indexOf(actual.id)
    const hasta = actual.ids.indexOf(Number(destino.dataset.categoriaId))
    if (hasta < 0 || desde === hasta) return
    const bounds = destino.getBoundingClientRect()
    const centro = bounds.top + bounds.height / 2
    if ((hasta > desde && event.clientY < centro) || (hasta < desde && event.clientY > centro)) return
    const ids = [...actual.ids]
    ids.splice(desde, 1)
    ids.splice(hasta, 0, actual.id)
    actual.ids = ids
    setVistaPrevia(ids)
    setAnuncio(`Posición ${hasta + 1} de ${ids.length}`)
  }
  const cancelar = () => {
    arrastre.current = null
    setArrastrando(null)
    setVistaPrevia(null)
  }
  const soltar = () => {
    const actual = arrastre.current
    if (!actual) return
    arrastre.current = null
    setArrastrando(null)
    if (actual.ids.some((id, index) => id !== categorias[index]?.id)) guardar(actual.ids)
    else setVistaPrevia(null)
  }
  const ayuda = <>
    <span id={instruccionesId} className="category-order-announcement">Arrastra el asa arriba o abajo. Con teclado usa las flechas. Escape cancela el arrastre.</span>
    <span role="status" className="category-order-announcement">{mutation.isPending ? 'Guardando orden…' : anuncio}</span>
  </>
  const asa = (categoria: Categoria) => <button className="category-drag-handle" disabled={mutation.isPending}
    aria-label={`Mover ${categoria.nombre}`} aria-describedby={instruccionesId} title="Arrastrar para ordenar"
    onPointerDown={event => iniciar(event, categoria.id)} onPointerMove={arrastrar} onPointerUp={soltar} onPointerCancel={cancelar}
    onLostPointerCapture={() => { if (arrastre.current) cancelar() }}
    onKeyDown={event => {
      if (event.key === 'Escape') { cancelar(); return }
      if (arrastre.current || mutation.isPending || !['ArrowUp', 'ArrowDown'].includes(event.key)) return
      event.preventDefault()
      const indice = categorias.findIndex(c => c.id === categoria.id)
      const destino = indice + (event.key === 'ArrowUp' ? -1 : 1)
      if (destino < 0 || destino >= categorias.length) return
      const ids = categorias.map(c => c.id)
      ids.splice(indice, 1)
      ids.splice(destino, 0, categoria.id)
      guardar(ids)
      setAnuncio(`${categoria.nombre}, posición ${destino + 1} de ${ids.length}`)
    }}><GripVertical size={22}/></button>
  return { lista, visibles, arrastrando, ayuda, asa }
}
