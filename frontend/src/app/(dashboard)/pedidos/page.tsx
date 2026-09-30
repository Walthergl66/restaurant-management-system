'use client'
import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { Plus, Search, RefreshCw } from 'lucide-react'
import api from '@/lib/api'
import { queryClient } from '@/lib/queryClient'
import { fmt, ESTADO_PEDIDO } from '@/lib/utils'
import type { PedidoResponse, EstadoPedido } from '@/types'
import toast from 'react-hot-toast'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'
import NuevoPedidoModal from '@/components/NuevoPedidoModal'

const ESTADOS: (EstadoPedido | 'TODOS')[] = ['TODOS','BORRADOR','CONFIRMADO','EN_PREPARACION','LISTO','ENTREGADO','CANCELADO']

export default function PedidosPage() {
  const router = useRouter()
  const [search, setSearch]   = useState('')
  const [filtro, setFiltro]   = useState<EstadoPedido | 'TODOS'>('TODOS')
  const [showModal, setShowModal] = useState(false)

  const { data: pedidos = [], isLoading, refetch } = useQuery<PedidoResponse[]>({
    queryKey: ['pedidos', 'list'],
    queryFn: async () => {
      const { data } = await api.get('/pedidos')
      return Array.isArray(data) ? data : data.content ?? []
    },
    refetchInterval: 15_000,
  })

  const confirmar = useMutation({
    mutationFn: (codigo: string) =>
      api.post(`/pedidos/${codigo}/confirmar`, null, {
        headers: { 'Idempotency-Key': `web-${codigo}-${Date.now()}` },
      }),
    onSuccess: () => { toast.success('Pedido confirmado'); queryClient.invalidateQueries({ queryKey: ['pedidos'] }) },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  const cancelar = useMutation({
    mutationFn: (codigo: string) => api.delete(`/pedidos/${codigo}`),
    onSuccess: () => { toast.success('Pedido cancelado'); queryClient.invalidateQueries({ queryKey: ['pedidos'] }) },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  const filtered = pedidos.filter(p => {
    const okEstado = filtro === 'TODOS' || p.estado === filtro
    const okSearch = !search || p.codigo.toLowerCase().includes(search.toLowerCase()) || (p.mesaNombre ?? '').toLowerCase().includes(search.toLowerCase())
    return okEstado && okSearch
  })

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:20 }}>
      {/* header */}
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:10 }}>
        <h1 style={{ fontSize:20, fontWeight:800 }}>Pedidos</h1>
        <div style={{ display:'flex', gap:8 }}>
          <button className="btn-ghost" onClick={() => refetch()}><RefreshCw size={15}/></button>
          <button className="btn-fuchsia" onClick={() => setShowModal(true)}><Plus size={15}/> Nuevo pedido</button>
        </div>
      </div>

      {/* filters */}
      <div style={{ display:'flex', flexWrap:'wrap', gap:8 }}>
        <div style={{ display:'flex', alignItems:'center', gap:8, background:'var(--surface)', border:'1px solid var(--border)', borderRadius:12, padding:'8px 14px', flex:1, minWidth:220 }}>
          <Search size={15} color="var(--muted)"/>
          <input
            className="input"
            style={{ border:'none', background:'none', padding:0, flex:1 }}
            placeholder="Buscar código, mesa…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
        <div style={{ display:'flex', gap:6, flexWrap:'wrap' }}>
          {ESTADOS.map(e => (
            <button
              key={e}
              onClick={() => setFiltro(e)}
              style={{
                padding:'8px 14px', borderRadius:10, fontSize:12, fontWeight:600, cursor:'pointer', border:'none',
                background: filtro === e ? 'var(--yellow)' : 'var(--surface)',
                color: filtro === e ? '#000' : 'var(--muted)',
                outline: filtro !== e ? '1px solid var(--border)' : 'none',
              }}
            >
              {e === 'TODOS' ? 'Todos' : ESTADO_PEDIDO[e]?.label ?? e}
            </button>
          ))}
        </div>
      </div>

      {/* table */}
      <div className="card" style={{ padding:0, overflow:'hidden' }}>
        {isLoading
          ? <p style={{ padding:32, textAlign:'center', color:'var(--dim)' }}>Cargando…</p>
          : filtered.length === 0
            ? <p style={{ padding:32, textAlign:'center', color:'var(--dim)' }}>Sin pedidos</p>
            : (
              <table className="tbl">
                <thead>
                  <tr>
                    {['Código','Mesa','Ítems','Total','Estado','Creado','Acciones'].map(h => <th key={h}>{h}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(p => (
                    <tr key={p.id} style={{ cursor:'pointer' }} onClick={() => router.push(`/pedidos/${p.codigo}`)}>
                      <td style={{ fontWeight:700, color:'var(--fuchsia)' }}>#{p.codigo}</td>
                      <td style={{ color:'var(--muted)' }}>{p.mesaNombre ?? '—'}</td>
                      <td style={{ color:'var(--muted)' }}>{p.lineas?.length ?? 0}</td>
                      <td style={{ fontWeight:700, color:'var(--yellow)' }}>{fmt(p.total)}</td>
                      <td><span className={`badge ${ESTADO_PEDIDO[p.estado]?.badge ?? ''}`}>{ESTADO_PEDIDO[p.estado]?.label ?? p.estado}</span></td>
                      <td style={{ fontSize:12, color:'var(--muted)' }}>{p.creadoAt ? format(new Date(p.creadoAt),'dd/MM/yy HH:mm',{locale:es}) : '—'}</td>
                      <td onClick={e => e.stopPropagation()}>
                        {p.estado === 'BORRADOR' && (
                          <div style={{ display:'flex', gap:6 }}>
                            <button className="btn-yellow" style={{ padding:'6px 12px', fontSize:12 }} onClick={() => confirmar.mutate(p.codigo)}>Confirmar</button>
                            <button className="btn-ghost" style={{ padding:'6px 12px', fontSize:12, color:'var(--error)' }} onClick={() => cancelar.mutate(p.codigo)}>Cancelar</button>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )
        }
      </div>

      {showModal && <NuevoPedidoModal onClose={() => setShowModal(false)} />}
    </div>
  )
}
