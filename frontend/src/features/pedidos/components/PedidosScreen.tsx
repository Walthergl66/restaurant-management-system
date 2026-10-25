'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Plus, Search, RefreshCw } from 'lucide-react'
import { fmt, ESTADO_PEDIDO } from '@/shared/lib/utils'
import { usePedidos } from '../hooks'
import type { EstadoPedido } from '../types'
import NuevoPedidoModal from './NuevoPedidoModal'
import { useAuthStore, hasPermiso } from '@/features/auth'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'
import QueryBoundary, { hasQueryIssue } from '@/shared/ui/QueryBoundary'

const ESTADOS: (EstadoPedido | 'TODOS')[] = ['TODOS','BORRADOR','CONFIRMADO','EN_PREPARACION','LISTO','ENTREGADO','ANULADO']

export default function PedidosScreen() {
  const router = useRouter()
  const [search, setSearch]   = useState('')
  const [filtro, setFiltro]   = useState<EstadoPedido | 'TODOS'>('TODOS')
  const [nuevo, setNuevo]     = useState(false)
  const user = useAuthStore(state => state.user)
  const puedeCrear = hasPermiso(user, 'pedidos:crear')

  const pedidosQuery = usePedidos()
  const { data: pedidos = [], isLoading, refetch } = pedidosQuery

  if (hasQueryIssue([pedidosQuery])) return <div className="page-stack"><h1>Pedidos</h1><QueryBoundary queries={[pedidosQuery]} label="los pedidos" /></div>

  const filtered = pedidos.filter(p => {
    const okEstado = filtro === 'TODOS' || p.estado === filtro
    const okSearch = !search
      || p.codigo.toLowerCase().includes(search.toLowerCase())
      || (p.mesaId != null && `mesa ${p.mesaId}`.includes(search.toLowerCase()))
    return okEstado && okSearch
  })

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:20 }}>
      {/* header */}
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:10 }}>
        <h1 style={{ fontSize:20, fontWeight:800 }}>Pedidos</h1>
        <div style={{ display:'flex', gap:8 }}>
          {puedeCrear && (
            <button className="btn-fuchsia" onClick={() => setNuevo(true)}><Plus size={15}/> Nuevo pedido</button>
          )}
          <button className="btn-ghost" onClick={() => refetch()} aria-label="Actualizar pedidos"><RefreshCw size={15}/></button>
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
                    {['Código','Mesa','Ítems','Total','Estado','Creado'].map(h => <th key={h}>{h}</th>)}
                  </tr>
                </thead>
                <tbody>
                  {filtered.map(p => (
                    <tr key={p.codigo} style={{ cursor:'pointer' }} onClick={() => router.push(`/pedidos/${p.codigo}`)}>
                      <td style={{ fontWeight:700, color:'var(--fuchsia)' }}>#{p.codigo}</td>
                      <td style={{ color:'var(--muted)' }}>{p.mesaId != null ? `Mesa ${p.mesaId}` : '—'}</td>
                      <td style={{ color:'var(--muted)' }}>{p.lineas?.length ?? 0}</td>
                      <td style={{ fontWeight:700, color:'var(--yellow)' }}>{fmt(p.total)}</td>
                      <td><span className={`badge ${ESTADO_PEDIDO[p.estado]?.badge ?? ''}`}>{ESTADO_PEDIDO[p.estado]?.label ?? p.estado}</span></td>
                      <td style={{ fontSize:12, color:'var(--muted)' }}>{p.createdAt ? format(new Date(p.createdAt),'dd/MM/yy HH:mm',{locale:es}) : '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )
        }
      </div>

      {nuevo && <NuevoPedidoModal onClose={() => setNuevo(false)} />}
    </div>
  )
}
