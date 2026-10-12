'use client'
import { useMemo, useState } from 'react'
import { Check, ImageIcon, Plus, Trash2, X } from 'lucide-react'
import { useGuardarProducto } from '../hooks'
import { normalizarNombreCatalogo } from '../normalizacion'
import type { Area, Categoria, Extra, Producto, ProductoPayload } from '../types'

type Errors = Partial<Record<'nombre' | 'precio' | 'categoriaId' | 'areaId' | 'imagenUrl', string>>

export default function ProductoModal({ categorias, areas, extras, editing, onClose }: {
  categorias: Categoria[]; areas: Area[]; extras: Extra[]; editing: Producto | null; onClose: () => void
}) {
  const categoriasActivas = categorias.filter(c => c.activo || c.id === editing?.categoriaId)
  const areasActivas = areas.filter(a => a.activo || a.id === editing?.areaId)
  const extrasActivos = extras.filter(e => e.activo || editing?.extras.some(x => x.id === e.id))
  const [nombre, setNombre] = useState(editing?.nombre ?? '')
  const [descripcion, setDescripcion] = useState(editing?.descripcion ?? '')
  const [imagenUrl, setImagenUrl] = useState(editing?.imagenUrl ?? '')
  const [precio, setPrecio] = useState(editing?.precio?.toString() ?? '')
  const [categoriaId, setCategoriaId] = useState<number>(editing?.categoriaId ?? 0)
  const [areaId, setAreaId] = useState<number>(editing?.areaId ?? 0)
  const [extraIds, setExtraIds] = useState<number[]>((editing?.extras ?? []).map(e => e.id))
  const [ingredientes, setIngredientes] = useState<string[]>(editing?.ingredientes ?? [])
  const [nuevoIngrediente, setNuevoIngrediente] = useState('')
  const [errors, setErrors] = useState<Errors>({})
  const [imageFailed, setImageFailed] = useState(false)
  const mutation = useGuardarProducto(onClose)
  const previewUrl = useMemo(() => imagenUrl.trim(), [imagenUrl])

  const addIngrediente = () => {
    const value = normalizarNombreCatalogo(nuevoIngrediente)
    if (value && !ingredientes.some(i => normalizarNombreCatalogo(i).toLocaleLowerCase('es') === value.toLocaleLowerCase('es'))) setIngredientes([...ingredientes, value])
    setNuevoIngrediente('')
  }

  const guardar = () => {
    const nextErrors: Errors = {}
    if (!nombre.trim()) nextErrors.nombre = 'El nombre es obligatorio.'
    const parsedPrice = Number(precio)
    if (precio === '' || !Number.isFinite(parsedPrice) || parsedPrice < 0) nextErrors.precio = 'Ingresa un precio válido.'
    if (!categoriaId) nextErrors.categoriaId = 'Selecciona una categoría.'
    if (!areaId) nextErrors.areaId = 'Selecciona un área de preparación.'
    if (imagenUrl && !/^https?:\/\//i.test(imagenUrl)) nextErrors.imagenUrl = 'Usa una URL que comience con http:// o https://.'
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) return
    const body: ProductoPayload = {
      nombre: normalizarNombreCatalogo(nombre), descripcion: descripcion.trim() || null, imagenUrl: imagenUrl.trim() || null,
      precio: parsedPrice, categoriaId, areaId, extraIds, ingredientes,
    }
    mutation.mutate({ id: editing?.id, body })
  }

  return <div className="overlay" role="dialog" aria-modal="true" aria-labelledby="producto-title"><div className="admin-modal admin-modal-lg">
    <div className="modal-header"><div><h2 id="producto-title">{editing ? 'Editar producto' : 'Nuevo producto'}</h2><p>Información que verá el cliente en el menú.</p></div><button className="icon-button" onClick={onClose} aria-label="Cerrar"><X size={18}/></button></div>
    <div className="product-form-grid">
      <div className="form-stack">
        <Field label="Nombre" error={errors.nombre}><input value={nombre} maxLength={120} onChange={e => setNombre(e.target.value)} onBlur={() => setNombre(normalizarNombreCatalogo(nombre))} className="input" autoFocus/></Field>
        <Field label="Descripción"><textarea value={descripcion} maxLength={500} onChange={e => setDescripcion(e.target.value)} className="input" rows={3}/></Field>
        <div className="form-grid-2">
          <Field label="Precio" error={errors.precio}><input type="number" min="0" step="0.01" value={precio} onChange={e => setPrecio(e.target.value)} className="input"/></Field>
          <Field label="Categoría" error={errors.categoriaId}><select value={categoriaId} onChange={e => setCategoriaId(Number(e.target.value))} className="input"><option value={0}>Seleccionar…</option>{categoriasActivas.map(c => <option key={c.id} value={c.id}>{c.nombre}{!c.activo ? ' (inactiva)' : ''}</option>)}</select></Field>
        </div>
        <Field label="Área de preparación" error={errors.areaId}><select value={areaId} onChange={e => setAreaId(Number(e.target.value))} className="input"><option value={0}>Seleccionar…</option>{areasActivas.map(a => <option key={a.id} value={a.id}>{a.nombre}{!a.activo ? ' (inactiva)' : ''}</option>)}</select></Field>
        <Field label="Extras permitidos"><div className="check-grid">{extrasActivos.length ? extrasActivos.map(extra => <label key={extra.id} className="check-option"><input type="checkbox" checked={extraIds.includes(extra.id)} onChange={() => setExtraIds(ids => ids.includes(extra.id) ? ids.filter(id => id !== extra.id) : [...ids, extra.id])}/><span>{extra.nombre}</span><small>${Number(extra.precio).toFixed(2)}</small></label>) : <p className="field-hint">No hay extras disponibles.</p>}</div></Field>
        <Field label="Ingredientes removibles"><div className="inline-control"><input className="input" value={nuevoIngrediente} placeholder="Ej. cebolla" onChange={e => setNuevoIngrediente(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addIngrediente() } }}/><button className="btn-ghost" type="button" onClick={addIngrediente}><Plus size={14}/>Agregar</button></div><div className="chip-list">{ingredientes.map(item => <span className="form-chip" key={item}>{item}<button type="button" onClick={() => setIngredientes(ingredientes.filter(i => i !== item))} aria-label={`Quitar ${item}`}><X size={12}/></button></span>)}</div></Field>
      </div>
      <div className="image-panel">
        <Field label="Imagen del producto" error={errors.imagenUrl}><input className="input" value={imagenUrl} maxLength={300} placeholder="https://servidor/imagen.webp" onChange={e => { setImagenUrl(e.target.value); setImageFailed(false) }}/><p className="field-hint">Por ahora ingresa una URL. El cargador se conectará al futuro servidor de archivos.</p></Field>
        <div className="image-preview">{previewUrl && !imageFailed ? <img // eslint-disable-line @next/next/no-img-element -- URL externa dinámica del gestor de archivos
          src={previewUrl} alt="Vista previa del producto" onError={() => setImageFailed(true)}/> : <><ImageIcon size={36}/><span>{imageFailed ? 'No se pudo cargar la imagen' : 'Vista previa'}</span></>}</div>
        {previewUrl && <button className="btn-ghost danger-text" type="button" onClick={() => { setImagenUrl(''); setImageFailed(false) }}><Trash2 size={14}/>Quitar imagen</button>}
      </div>
    </div>
    <div className="modal-actions"><button className="btn-yellow" onClick={guardar} disabled={mutation.isPending}><Check size={14}/>{mutation.isPending ? 'Guardando…' : 'Guardar producto'}</button><button className="btn-ghost" onClick={onClose}>Cancelar</button></div>
  </div></div>
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return <div className="form-field"><span>{label}</span>{children}{error && <small className="field-error">{error}</small>}</div>
}
