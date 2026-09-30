'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import { RefreshCw, Clock, CheckCircle } from 'lucide-react'
import api from '@/lib/api'
import { queryClient } from '@/lib/queryClient'
import type { ComandaResponse, ComandaEstado } from '@/types'
import toast from 'react-hot-toast'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'

const COLS: { estado: ComandaEstado; label: string; color: string; bg: string }[] = [
  { estado: 'PENDIENTE',      label: 'Pendientes', color: '#F5A623', bg: 'rgba(245,166,35,.07)'  },
  { estado: 'EN_PREPARACION', label: 'Preparando', color: '#E91E8C', bg: 'rgba(233,30,140,.07)' },
  { estado: 'LISTA',          label: 'Listos',     color: '#22c55e', bg: 'rgba(34,197,94,.07)'   },
]

export default function CocinaPage() {
  const { data: comandas = [], isLoading, refetch } = useQuery<ComandaResponse[]>({
    queryKey: ['comandas'],
    queryFn: async () => { const { data } = await api.get('/comandas'); return data },
    refetchInterval: 10_000,
  })

  const enPrep = useMutation({
    mutationFn: (id: number) => api.post(`/comandas/${id}/en-preparacion`),
    onSuccess: () => { toast.success('En preparación'); queryClient.invalidateQueries({ queryKey: ['comandas'] }) },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  const listo = useMutation({
    mutationFn: (id: number) => api.post(`/comandas/${id}/listo`),
    onSuccess: () => { toast.success('¡Lista!'); queryClient.invalidateQueries({ queryKey: ['comandas'] }) },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:10 }}>
        <h1 style={{ fontSize:20, fontWeight:800 }}>Cocina — Comandas</h1>
        <button className="btn-ghost" onClick={() => refetch()}><RefreshCw size={15}/> Actualizar</button>
      </div>

      {/* kpis */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:12 }}>
        {COLS.map(c => (
          <div key={c.estado} className="card" style={{ textAlign:'center' }}>
            <p style={{ fontSize:30, fontWeight:900, color:c.color }}>{comandas.filter(x => x.estado === c.estado).length}</p>
            <p style={{ fontSize:12, color:'var(--muted)', marginTop:2 }}>{c.label}</p>
          </div>
        ))}
      </div>

      {isLoading
        ? <p style={{ color:'var(--dim)', textAlign:'center', padding:32 }}>Cargando…</p>
        : (
          <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:16 }}>
            {COLS.map(col => {
              const items = comandas.filter(c => c.estado === col.estado)
              return (
                <div key={col.estado} style={{ background:col.bg, border:`1px solid ${col.color}33`, borderRadius:16, padding:16, minHeight:200 }}>
                  <div style={{ display:'flex', alignItems:'center', gap:8, marginBottom:14 }}>
                    <span style={{ width:10, height:10, borderRadius:'50%', background:col.color }}/>
                    <h2 style={{ fontWeight:700, fontSize:14, color:col.color }}>{col.label}</h2>
                    <span style={{ marginLeft:'auto', fontSize:11, fontWeight:700, background:`${col.color}22`, color:col.color, borderRadius:20, padding:'2px 8px' }}>{items.length}</span>
                  </div>

                  {items.length === 0
                    ? <p style={{ fontSize:12, color:'var(--dim)', textAlign:'center', padding:'24px 0' }}>Sin comandas</p>
                    : items.map(cmd => (
                        <div key={cmd.id} style={{ background:'var(--surface)', border:'1px solid var(--border)', borderRadius:12, padding:14, marginBottom:10 }}>
                          <div style={{ display:'flex', justifyContent:'space-between', marginBottom:6 }}>
                            <span style={{ fontSize:13, fontWeight:700, color:'var(--fuchsia)' }}>#{cmd.pedidoCodigo}</span>
                            <span style={{ fontSize:11, color:'var(--muted)' }}>{cmd.creadoAt ? format(new Date(cmd.creadoAt),'HH:mm',{locale:es}) : ''}</span>
                          </div>
                          <p style={{ fontSize:11, color:'var(--muted)', marginBottom:8 }}>{cmd.areaNombre}</p>
                          <div style={{ display:'flex', flexDirection:'column', gap:3, marginBottom:10 }}>
                            {cmd.items.map((item, i) => (
                              <div key={i} style={{ display:'flex', justifyContent:'space-between', fontSize:12 }}>
                                <span><strong>{item.cantidad}×</strong> {item.productoNombre}</span>
                                {item.anotaciones && <span style={{ color:'var(--muted)', fontSize:11 }}>{item.anotaciones}</span>}
                              </div>
                            ))}
                          </div>
                          <div style={{ display:'flex', gap:6 }}>
                            {cmd.estado === 'PENDIENTE' && (
                              <button className="btn-fuchsia" style={{ flex:1, justifyContent:'center', fontSize:11, padding:'6px 8px' }} onClick={() => enPrep.mutate(cmd.id)}>
                                <Clock size={11}/> Preparar
                              </button>
                            )}
                            {cmd.estado === 'EN_PREPARACION' && (
                              <button style={{ flex:1, justifyContent:'center', fontSize:11, padding:'6px 8px', background:'#22c55e', color:'#000', border:'none', borderRadius:8, fontWeight:700, cursor:'pointer', display:'flex', alignItems:'center', gap:4 }} onClick={() => listo.mutate(cmd.id)}>
                                <CheckCircle size={11}/> Listo
                              </button>
                            )}
                          </div>
                        </div>
                      ))
                  }
                </div>
              )
            })}
          </div>
        )
      }
    </div>
  )
}
