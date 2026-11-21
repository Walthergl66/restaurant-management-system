'use client'
import { useState } from 'react'
import { ArrowDown, ArrowUp, MapPin, Pencil, Plus, Trash2 } from 'lucide-react'
import ConfirmDialog from '@/shared/ui/ConfirmDialog'
import { useEliminarZona, useOrdenarZonas } from '../hooks'
import { moverZona } from '../zonas'
import type { Zona } from '../types'
import ZonaModal from './ZonaModal'

/** Gestión de las zonas del restaurante: crear, renombrar, ordenar y eliminar (solo si están vacías). */
export default function ZonasPanel({ zonas }: { zonas: Zona[] }) {
  const [modal, setModal] = useState<{ editing: Zona | null } | null>(null)
  const [eliminando, setEliminando] = useState<Zona | null>(null)
  const ordenar = useOrdenarZonas()
  const eliminar = useEliminarZona()

  const ids = zonas.map(z => z.id)
  const mover = (indice: number, delta: -1 | 1) => ordenar.mutate(moverZona(ids, indice, delta))

  const motivoNoEliminable = (zona: Zona) => {
    if (zona.cantidadMesas > 0) return 'No se puede eliminar: la zona tiene mesas. Muévelas a otra zona primero.'
    if (zonas.length <= 1) return 'Debe existir al menos una zona.'
    return null
  }

  return (
    <section className="card" style={{ display: 'flex', flexDirection: 'column', gap: 12 }} aria-labelledby="zonas-titulo">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
        <div>
          <h2 id="zonas-titulo" style={{ fontSize: 15, fontWeight: 800, display: 'flex', alignItems: 'center', gap: 6 }}>
            <MapPin size={15} /> Zonas del restaurante
          </h2>
          <p style={{ color: 'var(--muted)', fontSize: 12, marginTop: 4 }}>El mesero las ve en este orden.</p>
        </div>
        <button className="btn-ghost" onClick={() => setModal({ editing: null })}><Plus size={14} /> Nueva zona</button>
      </div>

      <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'flex', flexDirection: 'column', gap: 8 }}>
        {zonas.map((zona, indice) => {
          const motivo = motivoNoEliminable(zona)
          return (
            <li key={zona.id} style={{ display: 'flex', alignItems: 'center', gap: 10, background: 'var(--surface-2)', borderRadius: 12, padding: '8px 12px' }}>
              <span style={{ fontWeight: 700, flex: 1 }}>{zona.nombre}</span>
              <span style={{ color: 'var(--muted)', fontSize: 12 }}>
                {zona.cantidadMesas} {zona.cantidadMesas === 1 ? 'mesa' : 'mesas'}
              </span>
              <button className="icon-button" aria-label={`Subir ${zona.nombre}`} disabled={indice === 0 || ordenar.isPending} onClick={() => mover(indice, -1)}><ArrowUp size={15} /></button>
              <button className="icon-button" aria-label={`Bajar ${zona.nombre}`} disabled={indice === zonas.length - 1 || ordenar.isPending} onClick={() => mover(indice, 1)}><ArrowDown size={15} /></button>
              <button className="icon-button" aria-label={`Renombrar ${zona.nombre}`} onClick={() => setModal({ editing: zona })}><Pencil size={15} /></button>
              <button
                className="icon-button"
                aria-label={`Eliminar ${zona.nombre}`}
                title={motivo ?? 'Eliminar zona'}
                disabled={!!motivo}
                onClick={() => setEliminando(zona)}
              >
                <Trash2 size={15} />
              </button>
            </li>
          )
        })}
      </ul>

      {modal && <ZonaModal editing={modal.editing} onClose={() => setModal(null)} />}

      {eliminando && (
        <ConfirmDialog
          title={`Eliminar zona ${eliminando.nombre}`}
          message={`¿Seguro que quieres eliminar la zona ${eliminando.nombre}? Esta acción no se puede deshacer.`}
          confirmText="Eliminar"
          variant="danger"
          pending={eliminar.isPending}
          onConfirm={() => eliminar.mutate(eliminando.id, { onSuccess: () => setEliminando(null) })}
          onClose={() => setEliminando(null)}
        />
      )}
    </section>
  )
}
