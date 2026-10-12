'use client'
import { useState } from 'react'
import { Check, X } from 'lucide-react'
import { useGuardarArea, useGuardarCategoria, useGuardarExtra } from '../hooks'
import type { Area, Categoria, Extra } from '../types'

type Kind = 'categoria' | 'area' | 'extra'
type Item = Categoria | Area | Extra

export default function CatalogoModal({ kind, editing, onClose }: { kind: Kind; editing: Item | null; onClose: () => void }) {
  const [nombre, setNombre] = useState(editing?.nombre ?? '')
  const [descripcion, setDescripcion] = useState(editing?.descripcion ?? '')
  const [orden, setOrden] = useState(editing && 'orden' in editing ? String(editing.orden) : '0')
  const [precio, setPrecio] = useState(editing && 'precio' in editing ? String(editing.precio) : '')
  const [error, setError] = useState('')
  const categoriaMutation = useGuardarCategoria(onClose)
  const areaMutation = useGuardarArea(onClose)
  const extraMutation = useGuardarExtra(onClose)
  const mutation = kind === 'categoria' ? categoriaMutation : kind === 'area' ? areaMutation : extraMutation
  const guardar = () => {
    if (!nombre.trim()) return setError('El nombre es obligatorio.')
    if (kind === 'extra' && (precio === '' || Number(precio) < 0)) return setError('Ingresa un precio válido.')
    setError('')
    const base = { nombre: nombre.trim(), descripcion: descripcion.trim() || null }
    if (kind === 'categoria') categoriaMutation.mutate({ id: editing?.id, body: { ...base, orden: Number(orden) || 0 } })
    else if (kind === 'area') areaMutation.mutate({ id: editing?.id, body: base })
    else extraMutation.mutate({ id: editing?.id, body: { ...base, precio: Number(precio) } })
  }
  const title = kind === 'categoria' ? 'categoría' : kind === 'area' ? 'área de preparación' : 'extra'
  return <div className="overlay" role="dialog" aria-modal="true"><div className="admin-modal">
    <div className="modal-header"><div><h2>{editing ? 'Editar' : 'Nueva'} {title}</h2><p>Completa los datos del catálogo.</p></div><button className="icon-button" onClick={onClose} aria-label="Cerrar"><X size={18}/></button></div>
    <div className="form-stack"><label className="form-field"><span>Nombre</span><input className="input" value={nombre} maxLength={80} onChange={e => setNombre(e.target.value)} autoFocus/>{error && <small className="field-error">{error}</small>}</label><label className="form-field"><span>Descripción</span><textarea className="input" rows={3} maxLength={200} value={descripcion} onChange={e => setDescripcion(e.target.value)}/></label>{kind === 'categoria' && <label className="form-field"><span>Orden en el menú</span><input className="input" type="number" min="0" value={orden} onChange={e => setOrden(e.target.value)}/></label>}{kind === 'extra' && <label className="form-field"><span>Precio adicional</span><input className="input" type="number" min="0" step="0.01" value={precio} onChange={e => setPrecio(e.target.value)}/></label>}</div>
    <div className="modal-actions"><button className="btn-yellow" onClick={guardar} disabled={mutation.isPending}><Check size={14}/>{mutation.isPending ? 'Guardando…' : 'Guardar'}</button><button className="btn-ghost" onClick={onClose}>Cancelar</button></div>
  </div></div>
}
