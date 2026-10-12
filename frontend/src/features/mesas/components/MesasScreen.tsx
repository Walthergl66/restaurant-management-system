'use client'
import { useState } from 'react'
import { Plus, Users, X } from 'lucide-react'
import { useMesas, useCambiarEstadoMesa, useEliminarMesa } from '../hooks'
import type { Mesa, EstadoMesa } from '../types'
import MesaModal from './MesaModal'

const ESTADO_CFG: Record<EstadoMesa, { label: string; color: string; bg: string }> = {
  LIBRE:     { label: 'Libre',     color: '#22c55e', bg: 'rgba(34,197,94,.12)'  },
  OCUPADA:   { label: 'Ocupada',   color: '#E91E8C', bg: 'rgba(233,30,140,.12)' },
  RESERVADA: { label: 'Reservada', color: '#F5A623', bg: 'rgba(245,166,35,.12)' },
  INACTIVA:  { label: 'Inactiva',  color: '#555',    bg: 'rgba(80,80,80,.12)'   },
}

export default function MesasScreen() {
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing]   = useState<Mesa | null>(null)

  const { data: mesas = [], isLoading } = useMesas()
  const cambiarEstado = useCambiarEstadoMesa()
  const eliminar = useEliminarMesa()

  const kpis = [
    { label: 'Libres',     count: mesas.filter(m => m.estado === 'LIBRE').length,     color: '#22c55e' },
    { label: 'Ocupadas',   count: mesas.filter(m => m.estado === 'OCUPADA').length,   color: '#E91E8C' },
    { label: 'Reservadas', count: mesas.filter(m => m.estado === 'RESERVADA').length, color: '#F5A623' },
  ]

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:10 }}>
        <h1 style={{ fontSize:20, fontWeight:800 }}>Mesas</h1>
        <button className="btn-fuchsia" onClick={() => { setEditing(null); setShowForm(true) }}><Plus size={15}/> Nueva mesa</button>
      </div>

      {/* kpis */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
        {kpis.map(k => (
          <div key={k.label} className="card" style={{ textAlign:'center', padding:'16px 12px' }}>
            <p style={{ fontSize:32, fontWeight:900, color:k.color }}>{k.count}</p>
            <p style={{ fontSize:12, color:'var(--muted)', marginTop:2 }}>{k.label}</p>
          </div>
        ))}
      </div>

      {/* grid mesas */}
      {isLoading
        ? <p style={{ color:'var(--dim)', textAlign:'center', padding:32 }}>Cargando…</p>
        : (
          <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(155px,1fr))', gap:12 }}>
            {mesas.map(m => {
              const cfg = ESTADO_CFG[m.estado]
              return (
                <div key={m.id} style={{ background:'var(--surface)', border:`1px solid ${cfg.color}44`, borderRadius:16, padding:16, display:'flex', flexDirection:'column', gap:10 }}>
                  <span style={{ fontSize:11, fontWeight:700, background:cfg.bg, color:cfg.color, borderRadius:20, padding:'2px 8px', width:'fit-content' }}>{cfg.label}</span>
                  <div style={{ textAlign:'center' }}>
                    <p style={{ fontSize:22, fontWeight:900 }}>Mesa {m.numero}</p>
                    {m.ubicacion && <p style={{ fontSize:11, color:'var(--muted)' }}>{m.ubicacion}</p>}
                    <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:4, marginTop:4 }}>
                      <Users size={12} color="var(--muted)"/>
                      <span style={{ fontSize:11, color:'var(--muted)' }}>{m.capacidad} personas</span>
                    </div>
                  </div>
                  <select
                    value={m.estado}
                    onChange={e => cambiarEstado.mutate({ id: m.id, estado: e.target.value as EstadoMesa })}
                    className="input"
                    style={{ fontSize:12, padding:'6px 10px' }}
                  >
                    {(Object.keys(ESTADO_CFG) as EstadoMesa[]).map(e => (
                      <option key={e} value={e}>{ESTADO_CFG[e].label}</option>
                    ))}
                  </select>
                  <div style={{ display:'flex', gap:6 }}>
                    <button className="btn-ghost" style={{ flex:1, fontSize:11, padding:'6px 8px', justifyContent:'center' }} onClick={() => { setEditing(m); setShowForm(true) }}>Editar</button>
                    <button className="btn-ghost" style={{ padding:'6px 8px', fontSize:11, color:'var(--error)' }} onClick={() => { if (confirm(`¿Eliminar la mesa ${m.numero}?`)) eliminar.mutate(m.id) }}><X size={12}/></button>
                  </div>
                </div>
              )
            })}
          </div>
        )
      }

      {showForm && <MesaModal editing={editing} onClose={() => setShowForm(false)} />}
    </div>
  )
}
