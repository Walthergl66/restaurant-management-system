'use client'
import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { Plus, Users, X, Check } from 'lucide-react'
import api from '@/lib/api'
import { queryClient } from '@/lib/queryClient'
import type { Mesa, EstadoMesa } from '@/types'
import toast from 'react-hot-toast'

const ESTADO_CFG: Record<EstadoMesa, { label: string; color: string; bg: string }> = {
  DISPONIBLE:        { label: 'Disponible',       color: '#22c55e', bg: 'rgba(34,197,94,.12)'  },
  OCUPADA:           { label: 'Ocupada',           color: '#E91E8C', bg: 'rgba(233,30,140,.12)' },
  RESERVADA:         { label: 'Reservada',         color: '#F5A623', bg: 'rgba(245,166,35,.12)' },
  FUERA_DE_SERVICIO: { label: 'Fuera de servicio', color: '#555',    bg: 'rgba(80,80,80,.12)'   },
}

export default function MesasPage() {
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing]   = useState<Mesa | null>(null)

  const { data: mesas = [], isLoading } = useQuery<Mesa[]>({
    queryKey: ['mesas'],
    queryFn: async () => {
      const { data } = await api.get('/mesas')
      return Array.isArray(data) ? data : data.content ?? []
    },
    refetchInterval: 20_000,
  })

  const cambiarEstado = useMutation({
    mutationFn: ({ id, estado }: { id: number; estado: EstadoMesa }) => api.patch(`/mesas/${id}/estado`, { estado }),
    onSuccess: () => { toast.success('Estado actualizado'); queryClient.invalidateQueries({ queryKey: ['mesas'] }) },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  const eliminar = useMutation({
    mutationFn: (id: number) => api.delete(`/mesas/${id}`),
    onSuccess: () => { toast.success('Mesa eliminada'); queryClient.invalidateQueries({ queryKey: ['mesas'] }) },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  const kpis = [
    { label: 'Disponibles', count: mesas.filter(m => m.estado === 'DISPONIBLE').length, color: '#22c55e' },
    { label: 'Ocupadas',    count: mesas.filter(m => m.estado === 'OCUPADA').length,    color: '#E91E8C' },
    { label: 'Reservadas',  count: mesas.filter(m => m.estado === 'RESERVADA').length,  color: '#F5A623' },
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
                    <p style={{ fontSize:22, fontWeight:900 }}>{m.nombre}</p>
                    {m.zona && <p style={{ fontSize:11, color:'var(--muted)' }}>{m.zona}</p>}
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
                    <button className="btn-ghost" style={{ padding:'6px 8px', fontSize:11, color:'var(--error)' }} onClick={() => { if (confirm(`¿Eliminar "${m.nombre}"?`)) eliminar.mutate(m.id) }}><X size={12}/></button>
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

function MesaModal({ editing, onClose }: { editing: Mesa | null; onClose: () => void }) {
  const [nombre,    setNombre]    = useState(editing?.nombre ?? '')
  const [capacidad, setCapacidad] = useState(editing?.capacidad?.toString() ?? '4')
  const [zona,      setZona]      = useState(editing?.zona ?? '')

  const mutation = useMutation({
    mutationFn: () => {
      const body = { nombre, capacidad: parseInt(capacidad), zona }
      return editing ? api.put(`/mesas/${editing.id}`, body) : api.post('/mesas', body)
    },
    onSuccess: () => { toast.success(editing ? 'Mesa actualizada' : 'Mesa creada'); queryClient.invalidateQueries({ queryKey: ['mesas'] }); onClose() },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  return (
    <div className="overlay">
      <div style={{ width:'100%', maxWidth:380, background:'var(--surface)', border:'1px solid var(--border)', borderRadius:20, padding:24 }}>
        <div style={{ display:'flex', justifyContent:'space-between', marginBottom:20 }}>
          <h2 style={{ fontWeight:700, fontSize:16 }}>{editing ? 'Editar mesa' : 'Nueva mesa'}</h2>
          <button onClick={onClose} style={{ background:'none', border:'none', cursor:'pointer', color:'var(--muted)' }}><X size={18}/></button>
        </div>
        <div style={{ display:'flex', flexDirection:'column', gap:14 }}>
          {[
            { label:'NOMBRE',            value:nombre,    set:setNombre,    type:'text'   },
            { label:'CAPACIDAD',         value:capacidad, set:setCapacidad, type:'number' },
            { label:'ZONA (opcional)',   value:zona,      set:setZona,      type:'text'   },
          ].map(({ label, value, set, type }) => (
            <div key={label}>
              <label style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:6 }}>{label}</label>
              <input type={type} value={value} onChange={e => set(e.target.value)} className="input"/>
            </div>
          ))}
        </div>
        <div style={{ display:'flex', gap:8, marginTop:20 }}>
          <button className="btn-yellow" style={{ flex:1, justifyContent:'center' }} onClick={() => mutation.mutate()} disabled={mutation.isPending || !nombre}>
            <Check size={14}/> {mutation.isPending ? 'Guardando…' : 'Guardar'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}
