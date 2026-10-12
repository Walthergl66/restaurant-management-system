'use client'
import { useState } from 'react'
import { Plus, Search, Pencil, Trash2 } from 'lucide-react'
import { useProductos, useCategorias, useEliminarProducto } from '../hooks'
import type { Producto } from '../types'
import ProductoModal from './ProductoModal'

export default function ProductosScreen() {
  const [search, setSearch]         = useState('')
  const [catFiltro, setCatFiltro]   = useState<number | 'TODOS'>('TODOS')
  const [showForm, setShowForm]     = useState(false)
  const [editing, setEditing]       = useState<Producto | null>(null)

  const { data: productos = [], isLoading } = useProductos()
  const { data: categorias = [] } = useCategorias()
  const eliminar = useEliminarProducto()

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
              <div key={p.id} style={{ background:'var(--surface)', border:'1px solid var(--border)', borderRadius:16, padding:16, display:'flex', flexDirection:'column', gap:8, opacity: p.activo ? 1 : .55 }}>
                <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start' }}>
                  <span style={{ fontSize:10, background:'var(--surface-2)', color:'var(--muted)', borderRadius:20, padding:'2px 8px' }}>{p.categoriaNombre ?? '—'}</span>
                  {!p.activo && <span className="badge badge-cancelado">Inactivo</span>}
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
