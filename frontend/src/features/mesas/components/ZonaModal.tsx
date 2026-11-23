'use client'
import { useState } from 'react'
import { Check, X } from 'lucide-react'
import { useGuardarZona } from '../hooks'
import type { Zona } from '../types'

const MAX_NOMBRE = 60

/** Crea o renombra una zona. `onSaved` recibe la zona guardada (p. ej. para seleccionarla en el modal de mesa). */
export default function ZonaModal({ editing, onClose, onSaved }: {
  editing: Zona | null
  onClose: () => void
  onSaved?: (zona: Zona) => void
}) {
  const [nombre, setNombre] = useState(editing?.nombre ?? '')
  const [error, setError] = useState('')
  const mutation = useGuardarZona(zona => {
    onSaved?.(zona)
    onClose()
  })

  const guardar = () => {
    const limpio = nombre.trim()
    if (!limpio) return setError('El nombre es obligatorio.')
    setError('')
    mutation.mutate({ id: editing?.id, nombre: limpio })
  }

  return (
    <div className="overlay" role="dialog" aria-modal="true">
      <div className="admin-modal">
        <div className="modal-header">
          <div>
            <h2>{editing ? 'Renombrar zona' : 'Nueva zona'}</h2>
            <p>Las zonas agrupan las mesas del restaurante (Salón, Terraza, Patio…).</p>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="Cerrar"><X size={18} /></button>
        </div>
        <div className="form-stack">
          <label className="form-field">
            <span>Nombre</span>
            <input
              className="input"
              value={nombre}
              maxLength={MAX_NOMBRE}
              onChange={e => setNombre(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && guardar()}
              autoFocus
            />
            {error && <small className="field-error">{error}</small>}
          </label>
        </div>
        <div className="modal-actions">
          <button className="btn-yellow" onClick={guardar} disabled={mutation.isPending}>
            <Check size={14} />{mutation.isPending ? 'Guardando…' : 'Guardar'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}
