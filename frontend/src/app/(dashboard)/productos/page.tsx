'use client'
import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { Plus, Search, Pencil, Trash2, X, Check } from 'lucide-react'
import api from '@/lib/api'
import { queryClient } from '@/lib/queryClient'
import type { Producto, Categoria } from '@/types'
import toast from 'react-hot-toast'

export default function ProductosPage() {
  const [search, setSearch]         = useState('')
  const [catFiltro, setCatFiltro]   = useState<number | 'TODOS'>('TODOS')
  const [showForm, setShowForm]     = useState(false)
  const [editing, setEditing]       = useState<Producto | null>(null)

  const { data: productos = [], isLoading } = useQuery<Producto[]>({
    queryKey: ['productos'],
    queryFn: async () => {
      const { data } = await api.get('/productos')
      return Array.isArray(data) ? data : data.content ?? []
    },
  })

  const { data: categorias = [] } = useQuery<Categoria[]>({
    queryKey: ['categorias'],
    queryFn: async () => {
      const { data } = await api.get('/categorias')
      return Array.isArray(data) ? data : data.content ?? []
    },
  })

  const eliminar = useMutation({
    mutationFn: (id: number) => api.delete(`/productos/${id}`),
    onSuccess: () => { toast.success('Eliminado'); queryClient.invalidateQueries({ queryKey: ['productos'] }) },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  const filtered = productos.filter(p => {
    const okCat    = catFiltro === 'TODOS' || p.categoriaId === catFiltro
    const okSearch = !search || p.nombre.toLowerCase().includes(search.toLowerCase())
    return okCat && okSearch
  })

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:10 }}>
        <h1 style={{ fontSize:20, fontWeight:800 }}>Productos</h1>
        <button className="btn-fuchsia" onClick={() => { setEditing(null); setShowForm(true) }}><Plus size={15}/> Nuevo producto</button>
      </div>

      {/* filtros */}
      <div style={{ display:'flex', flexWrap:'wrap', gap:8 }}>
        <div style={{ display:'flex', alignItems:'center', gap:8, background:'var(--surface)', border:'1px solid var(--border)', borderRadius:12, padding:'8px 14px', flex:1, minWidth:200 }}>
          <Search size={15} color="var(--muted)"/>
          <input className="input" style={{ border:'none', background:'none', padding:0, flex:1 }} placeholder="Buscar producto…" value={search} onChange={e => setSearch(e.target.value)}/>
        </div>
        <div style={{ display:'flex', gap:6, flexWrap:'wrap' }}>
          {[{ id: 'TODOS' as const, nombre:'Todos' }, ...categorias].map(c => (
            <button
              key={c.id}
              onClick={() => setCatFiltro(c.id as number | 'TODOS')}
              style={{ padding:'8px 14px', borderRadius:10, fontSize:12, fontWeight:600, cursor:'pointer', border:'none',
                background: catFiltro === c.id ? 'var(--yellow)' : 'var(--surface)',
                color: catFiltro === c.id ? '#000' : 'var(--muted)',
                outline: catFiltro !== c.id ? '1px solid var(--border)' : 'none',
              }}
            >{c.nombre}</button>
          ))}
        </div>
      </div>

      {/* grid */}
      {isLoading
        ? <p style={{ color:'var(--dim)', textAlign:'center', padding:32 }}>Cargando…</p>
        : (
          <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(180px,1fr))', gap:14 }}>
            {filtered.map(p => (
              <div key={p.id} style={{ background:'var(--surface)', border:`1px solid ${p.disponible ? 'var(--border)' : 'rgba(239,68,68,.3)'}`, borderRadius:16, padding:16, display:'flex', flexDirection:'column', gap:8, opacity: p.activo ? 1 : .55 }}>
                <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start' }}>
                  <span style={{ fontSize:10, background:'var(--surface-2)', color:'var(--muted)', borderRadius:20, padding:'2px 8px' }}>{p.categoriaNombre ?? '—'}</span>
                  {!p.disponible && <span className="badge badge-cancelado">Agotado</span>}
                </div>
                <p style={{ fontSize:14, fontWeight:700 }}>{p.nombre}</p>
                {p.descripcion && <p style={{ fontSize:12, color:'var(--muted)', overflow:'hidden', display:'-webkit-box', WebkitLineClamp:2, WebkitBoxOrient:'vertical' }}>{p.descripcion}</p>}
                <p style={{ fontSize:20, fontWeight:900, color:'var(--yellow)', marginTop:'auto' }}>${p.precio?.toFixed(2)}</p>
                <div style={{ display:'flex', gap:6 }}>
                  <button className="btn-ghost" style={{ flex:1, fontSize:11, padding:'6px 8px', justifyContent:'center' }} onClick={() => { setEditing(p); setShowForm(true) }}><Pencil size={11}/> Editar</button>
                  <button className="btn-ghost" style={{ padding:'6px 8px', color:'var(--error)' }} onClick={() => { if (confirm(`¿Eliminar "${p.nombre}"?`)) eliminar.mutate(p.id) }}><Trash2 size={11}/></button>
                </div>
              </div>
            ))}
          </div>
        )
      }

      {showForm && <ProductoModal categorias={categorias} editing={editing} onClose={() => setShowForm(false)} />}
    </div>
  )
}

function ProductoModal({ categorias, editing, onClose }: { categorias: Categoria[]; editing: Producto | null; onClose: () => void }) {
  const [nombre,      setNombre]      = useState(editing?.nombre ?? '')
  const [descripcion, setDescripcion] = useState(editing?.descripcion ?? '')
  const [precio,      setPrecio]      = useState(editing?.precio?.toString() ?? '')
  const [categoriaId, setCategoriaId] = useState<number>(editing?.categoriaId ?? (categorias[0]?.id ?? 0))
  const [disponible,  setDisponible]  = useState(editing?.disponible ?? true)

  const mutation = useMutation({
    mutationFn: () => {
      const body = { nombre, descripcion, precio: parseFloat(precio), categoriaId, disponible }
      return editing ? api.put(`/productos/${editing.id}`, body) : api.post('/productos', body)
    },
    onSuccess: () => { toast.success(editing ? 'Actualizado' : 'Creado'); queryClient.invalidateQueries({ queryKey: ['productos'] }); onClose() },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

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
          <label style={{ display:'flex', alignItems:'center', gap:10, cursor:'pointer' }}>
            <div onClick={() => setDisponible(!disponible)} style={{ width:40, height:22, borderRadius:11, background: disponible ? 'var(--yellow)' : 'var(--surface-3)', position:'relative', transition:'background .2s', cursor:'pointer' }}>
              <span style={{ position:'absolute', top:3, width:16, height:16, background:'#fff', borderRadius:'50%', transition:'transform .2s', transform: disponible ? 'translateX(21px)' : 'translateX(3px)' }}/>
            </div>
            <span style={{ fontSize:13, color:'var(--muted)' }}>Disponible</span>
          </label>
        </div>
        <div style={{ display:'flex', gap:8, marginTop:20 }}>
          <button className="btn-yellow" style={{ flex:1, justifyContent:'center' }} onClick={() => mutation.mutate()} disabled={mutation.isPending || !nombre || !precio}>
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
