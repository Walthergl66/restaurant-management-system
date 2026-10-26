'use client'
import { useEffect, useRef, useState } from 'react'
import { Check, Plus, X } from 'lucide-react'
import ArchivoSelector from '@/features/archivos/ArchivoSelector'
import { subirArchivo, descartarArchivo, type ArchivoSubido } from '@/features/archivos/api'
import { errMsg } from '@/shared/lib/utils'
import { useGuardarProducto } from '../hooks'
import { normalizarTextoCatalogo } from '../normalizacion'
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
  const [imagen, setImagen] = useState<File | null>(null)
  const [video, setVideo] = useState<File | null>(null)
  const [videoUrl, setVideoUrl] = useState(editing?.videoUrl ?? null)
  const [quitarVideo, setQuitarVideo] = useState(false)
  const [subiendo, setSubiendo] = useState(false)
  const [avance, setAvance] = useState(0)
  const [etapa, setEtapa] = useState('')
  const [errorSubida, setErrorSubida] = useState('')
  const guardado = useRef(false)
  const abort = useRef<AbortController | null>(null)
  const pendientes = useRef(new Map<File, ArchivoSubido>())
  const mutation = useGuardarProducto(() => { guardado.current = true; onClose() })
  const ocupado = subiendo || mutation.isPending
  useEffect(() => () => {
    abort.current?.abort()
    if (!guardado.current) for (const a of pendientes.current.values()) void descartarArchivo(a.id).catch(() => { /* El servidor reintenta la limpieza. */ })
  }, [])

  const addIngrediente = () => {
    const value = normalizarTextoCatalogo(nuevoIngrediente)
    if (value && !ingredientes.some(i => normalizarTextoCatalogo(i).toLocaleLowerCase('es') === value.toLocaleLowerCase('es'))) setIngredientes([...ingredientes, value])
    setNuevoIngrediente('')
  }

  const guardar = async () => {
    if (ocupado) return
    const nextErrors: Errors = {}
    if (!nombre.trim()) nextErrors.nombre = 'El nombre es obligatorio.'
    const parsedPrice = Number(precio)
    if (precio === '' || !Number.isFinite(parsedPrice) || parsedPrice < 0) nextErrors.precio = 'Ingresa un precio válido.'
    if (!categoriaId) nextErrors.categoriaId = 'Selecciona una categoría.'
    if (!areaId) nextErrors.areaId = 'Selecciona un área de preparación.'
    setErrors(nextErrors)
    if (Object.keys(nextErrors).length) return
    setErrorSubida(''); setSubiendo(true)
    abort.current = new AbortController()
    try {
      const subir = async (file: File, tipo: 'IMAGEN' | 'VIDEO') => {
        const anterior = pendientes.current.get(file)
        if (anterior) {
          if (anterior.tipo !== tipo) throw new Error('El contenido del archivo no corresponde al campo seleccionado.')
          return anterior
        }
        setAvance(0); setEtapa(tipo === 'IMAGEN' ? 'Subiendo imagen' : 'Subiendo video')
        const a = await subirArchivo(file, setAvance, abort.current!.signal)
        pendientes.current.set(file, a)
        if (a.tipo !== tipo) throw new Error('El contenido del archivo no corresponde al campo seleccionado.')
        return a
      }
      const nuevaImagen = imagen ? await subir(imagen, 'IMAGEN') : null
      const nuevoVideo = video ? await subir(video, 'VIDEO') : null
      const body: ProductoPayload = {
        nombre: normalizarTextoCatalogo(nombre), descripcion: normalizarTextoCatalogo(descripcion, 'descripcion') || null,
        imagenUrl: nuevaImagen?.url ?? (imagenUrl.trim() || null),
        imagenArchivoId: nuevaImagen?.id ?? (imagenUrl ? editing?.imagenArchivoId ?? null : null),
        videoArchivoId: nuevoVideo?.id ?? null, quitarVideo: !nuevoVideo && quitarVideo,
        precio: parsedPrice, categoriaId, areaId, extraIds, ingredientes,
      }
      setEtapa('Guardando producto')
      const usados = new Set([nuevaImagen?.id, nuevoVideo?.id])
      for (const [file, a] of pendientes.current) if (!usados.has(a.id)) {
        void descartarArchivo(a.id).catch(() => {}); pendientes.current.delete(file)
      }
      await mutation.mutateAsync({ id: editing?.id, body })
    } catch (error) {
      setErrorSubida(errMsg(error, 'No se pudo guardar. Los archivos anteriores del producto se conservan.'))
    } finally {
      setSubiendo(false)
    }
  }

  return <div className="overlay" role="dialog" aria-modal="true" aria-labelledby="producto-title"><div className="admin-modal admin-modal-lg product-editor">
    <div className="modal-header"><div><h2 id="producto-title">{editing ? 'Editar producto' : 'Nuevo producto'}</h2><p>Información que verá el cliente en el menú.</p></div><button className="icon-button" onClick={onClose} disabled={ocupado} aria-label="Cerrar"><X size={18}/></button></div>
    <fieldset className="product-form-grid media-fieldset" disabled={ocupado}>
      <div className="form-stack">
        <Field label="Nombre" error={errors.nombre}><input value={nombre} maxLength={120} onChange={e => setNombre(e.target.value)} onBlur={() => setNombre(normalizarTextoCatalogo(nombre))} className="input" autoFocus/></Field>
        <Field label="Descripción"><textarea value={descripcion} maxLength={500} onChange={e => setDescripcion(e.target.value)} onBlur={() => setDescripcion(normalizarTextoCatalogo(descripcion, 'descripcion'))} className="input" rows={3}/></Field>
        <div className="form-grid-2">
          <Field label="Precio" error={errors.precio}><input type="number" min="0" step="0.01" value={precio} onChange={e => setPrecio(e.target.value)} className="input"/></Field>
          <Field label="Categoría" error={errors.categoriaId}><select value={categoriaId} onChange={e => setCategoriaId(Number(e.target.value))} className="input"><option value={0}>Seleccionar…</option>{categoriasActivas.map(c => <option key={c.id} value={c.id}>{c.nombre}{!c.activo ? ' (inactiva)' : ''}</option>)}</select></Field>
        </div>
        <Field label="Área de preparación" error={errors.areaId}><select value={areaId} onChange={e => setAreaId(Number(e.target.value))} className="input"><option value={0}>Seleccionar…</option>{areasActivas.map(a => <option key={a.id} value={a.id}>{a.nombre}{!a.activo ? ' (inactiva)' : ''}</option>)}</select></Field>
        <Field label="Extras permitidos"><div className="check-grid">{extrasActivos.length ? extrasActivos.map(extra => <label key={extra.id} className="check-option"><input type="checkbox" checked={extraIds.includes(extra.id)} onChange={() => setExtraIds(ids => ids.includes(extra.id) ? ids.filter(id => id !== extra.id) : [...ids, extra.id])}/><span>{extra.nombre}</span><small>${Number(extra.precio).toFixed(2)}</small></label>) : <p className="field-hint">No hay extras disponibles.</p>}</div></Field>
        <Field label="Ingredientes removibles"><div className="inline-control"><input className="input" value={nuevoIngrediente} placeholder="Ej. cebolla" onChange={e => setNuevoIngrediente(e.target.value)} onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addIngrediente() } }}/><button className="btn-ghost" type="button" onClick={addIngrediente}><Plus size={14}/>Agregar</button></div><div className="chip-list">{ingredientes.map(item => <span className="form-chip" key={item}>{item}<button type="button" onClick={() => setIngredientes(ingredientes.filter(i => i !== item))} aria-label={`Quitar ${item}`}><X size={12}/></button></span>)}</div></Field>
      </div>
      <div className="image-panel">
        <ArchivoSelector tipo="IMAGEN" archivo={imagen} url={imagenUrl || null} disabled={ocupado} onChange={file => { setImagen(file); if (!file) setImagenUrl('') }} />
        <ArchivoSelector tipo="VIDEO" archivo={video} url={videoUrl} disabled={ocupado} onChange={file => { setVideo(file); setQuitarVideo(!file); if (!file) setVideoUrl(null) }} />
      </div>
    </fieldset>
    {ocupado && <div className="media-progress" role="status" aria-live="polite"><p>{etapa}{avance === 100 && etapa.startsWith('Subiendo') ? ' · Procesando archivo…' : '…'}</p>{etapa.startsWith('Subiendo') && <progress aria-label={etapa} max={100} value={avance} />}</div>}
    {errorSubida && <p className="field-error" role="alert">{errorSubida}</p>}
    <div className="modal-actions"><button className="btn-yellow" onClick={guardar} disabled={ocupado}><Check size={14}/>{ocupado ? 'Guardando…' : 'Guardar producto'}</button><button className="btn-ghost" onClick={onClose} disabled={ocupado}>Cancelar</button></div>
  </div></div>
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return <div className="form-field"><span>{label}</span>{children}{error && <small className="field-error">{error}</small>}</div>
}
