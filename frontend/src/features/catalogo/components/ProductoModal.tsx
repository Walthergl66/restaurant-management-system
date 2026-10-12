'use client'
import { useState } from 'react'
import { X, Check } from 'lucide-react'
import { useGuardarProducto } from '../hooks'
import type { Categoria, Producto, ProductoPayload } from '../types'

export default function ProductoModal({
  categorias,
  editing,
  onClose,
}: {
  categorias: Categoria[]
  editing: Producto | null
  onClose: () => void
}) {
  const [nombre,      setNombre]      = useState(editing?.nombre ?? '')
  const [descripcion, setDescripcion] = useState(editing?.descripcion ?? '')
  const [precio,      setPrecio]      = useState(editing?.precio?.toString() ?? '')
  const [categoriaId, setCategoriaId] = useState<number>(editing?.categoriaId ?? (categorias[0]?.id ?? 0))
  const [ingredientes, setIngredientes] = useState((editing?.ingredientes ?? []).join(', '))

  const mutation = useGuardarProducto(onClose)

  const guardar = () => {
    const body: ProductoPayload = {
      nombre,
      descripcion: descripcion || null,
      imagenUrl: editing?.imagenUrl ?? null,
      precio: parseFloat(precio),
      categoriaId: categoriaId || null,
      areaId: editing?.areaId ?? null,
      extraIds: (editing?.extras ?? []).map(e => e.id),
      ingredientes: ingredientes.split(',').map(s => s.trim()).filter(Boolean),
    }
    mutation.mutate({ id: editing?.id, body })
  }

  return (
    <div className="overlay">
      <div style={{ width:'100%', maxWidth:440, background:'var(--surface)', border:'1px solid var(--border)', borderRadius:20, padding:24 }}>
        <div style={{ display:'flex', justifyContent:'space-between', marginBottom:20 }}>
          <h2 style={{ fontWeight:700, fontSize:16 }}>{editing ? 'Editar producto' : 'Nuevo producto'}</h2>
          <button onClick={onClose} style={{ background:'none', border:'none', cursor:'pointer', color:'var(--muted)' }}><X size={18}/></button>
        </div>
        <div style={{ display:'flex', flexDirection:'column', gap:14 }}>
          <Field label="NOMBRE"><input value={nombre} onChange={e => setNombre(e.target.value)} className="input"/></Field>
          <Field label="DESCRIPCIÓN"><textarea value={descripcion} onChange={e => setDescripcion(e.target.value)} className="input" rows={2} style={{ resize:'none' }}/></Field>
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
            <Field label="PRECIO"><input type="number" step="0.01" value={precio} onChange={e => setPrecio(e.target.value)} className="input"/></Field>
            <Field label="CATEGORÍA">
              <select value={categoriaId} onChange={e => setCategoriaId(Number(e.target.value))} className="input">
                {categorias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}
              </select>
            </Field>
          </div>
          <Field label="INGREDIENTES (separados por coma)">
            <input value={ingredientes} onChange={e => setIngredientes(e.target.value)} placeholder="pan, carne, queso" className="input"/>
          </Field>
        </div>
        <div style={{ display:'flex', gap:8, marginTop:20 }}>
          <button className="btn-yellow" style={{ flex:1, justifyContent:'center' }} onClick={guardar} disabled={mutation.isPending || !nombre || !precio}>
            <Check size={14}/> {mutation.isPending ? 'Guardando…' : 'Guardar'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:6 }}>{label}</label>
      {children}
    </div>
  )
}
