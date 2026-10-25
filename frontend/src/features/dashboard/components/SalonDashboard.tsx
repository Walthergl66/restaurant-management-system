'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Table2, ShoppingBag, Receipt, ArrowRight, Plus } from 'lucide-react'
import type { ReactNode } from 'react'
import { fmt, ESTADO_PEDIDO, ESTADO_MESA } from '@/shared/lib/utils'
import { useMesas } from '@/features/mesas'
import { usePedidos, NuevoPedidoModal } from '@/features/pedidos'
import { useCuentas } from '@/features/cuentas'
import QueryBoundary, { hasQueryIssue } from '@/shared/ui/QueryBoundary'

/* ================================================================ */
export default function SalonDashboard() {
  const router = useRouter()
  const [nuevo, setNuevo] = useState(false)
  const mesasQuery = useMesas()
  const pedidosQuery = usePedidos()
  const cuentasQuery = useCuentas()
  const { data: mesas = [], isLoading: loadM } = mesasQuery
  const { data: pedidos = [], isLoading: loadP } = pedidosQuery
  const { data: cuentas = [], isLoading: loadC } = cuentasQuery

  const queries = [mesasQuery, pedidosQuery, cuentasQuery]
  if (hasQueryIssue(queries)) return <QueryBoundary queries={queries} label="el resumen del salón" />

  const mesasOcupadas = mesas.filter(m => m.estado === 'OCUPADA')
  const mesasLibres = mesas.filter(m => m.estado === 'LIBRE')
  const pedidosActivos = pedidos.filter(p => p.estado !== 'ANULADO')
  const porCobrar = cuentas.reduce((sum, c) => sum + c.total, 0)

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* action bar */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
        <button className="btn-fuchsia" onClick={() => setNuevo(true)}>
          <Plus size={15} /> Nuevo pedido
        </button>
        <button className="btn-ghost" onClick={() => router.push('/cuentas')}>
          <Receipt size={15} /> Ver cuentas
        </button>
      </div>

      {/* KPIs */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(190px,1fr))', gap: 14 }}>
        <KpiCard label="Mesas ocupadas" value={loadM ? '...' : `${mesasOcupadas.length}/${mesas.length}`} icon={<Table2 size={17}/>} color="var(--fuchsia)" />
        <KpiCard label="Mesas libres"   value={loadM ? '...' : String(mesasLibres.length)} icon={<Table2 size={17}/>} color="var(--success)" />
        <KpiCard label="Pedidos activos" value={loadP ? '...' : String(pedidosActivos.length)} icon={<ShoppingBag size={17}/>} color="var(--yellow)" />
        <KpiCard label="Por cobrar"     value={loadC ? '...' : fmt(porCobrar)} icon={<Receipt size={17}/>} color="var(--yellow)" />
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 280px', gap: 16 }}>

        {/* pedidos activos */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
            <h2 style={{ fontWeight: 700, fontSize: 15 }}>Pedidos activos</h2>
            <button onClick={() => router.push('/pedidos')} style={{
              display: 'flex', alignItems: 'center', gap: 4, fontSize: 12,
              fontWeight: 600, color: 'var(--yellow)', background: 'none', border: 'none', cursor: 'pointer',
            }}>
              Ver todos <ArrowRight size={13} />
            </button>
          </div>
          {loadP
            ? <p style={{ fontSize: 13, color: 'var(--dim)' }}>Cargando...</p>
            : pedidosActivos.length === 0
              ? <p style={{ fontSize: 13, color: 'var(--dim)' }}>No hay pedidos activos</p>
              : <div style={{ display: 'flex', flexDirection: 'column' }}>
                  {pedidosActivos.slice(0, 8).map((p) => (
                    <div
                      key={p.codigo}
                      onClick={() => router.push(`/pedidos/${p.codigo}`)}
                      style={{
                        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                        padding: '11px 0', borderBottom: '1px solid var(--border)', cursor: 'pointer',
                      }}
                    >
                      <div>
                        <p style={{ fontSize: 13, fontWeight: 700, color: 'var(--fuchsia)' }}>#{p.codigo}</p>
                        <p style={{ fontSize: 11, color: 'var(--muted)', marginTop: 1 }}>
                          {p.mesaId != null ? `Mesa ${p.mesaId}` : 'Sin mesa'} · {p.lineas?.slice(0,2).map(l=>l.nombreProducto).join(' + ')}
                        </p>
                        <p style={{ fontSize: 13, fontWeight: 700, marginTop: 1 }}>{fmt(p.total)}</p>
                      </div>
                      <span className={`badge ${ESTADO_PEDIDO[p.estado]?.badge ?? ''}`}>
                        {ESTADO_PEDIDO[p.estado]?.label ?? p.estado}
                      </span>
                    </div>
                  ))}
                </div>
          }
        </div>

        {/* mesas */}
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <h2 style={{ fontWeight: 700, fontSize: 15 }}>Salón</h2>

          <div style={{ background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: 12, padding: 14 }}>
            <p style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 600, marginBottom: 8, textTransform: 'uppercase', letterSpacing: 1 }}>Cuentas abiertas</p>
            <p style={{ fontSize: 26, fontWeight: 900, color: 'var(--yellow)' }}>{cuentas.length}</p>
            <p style={{ fontSize: 12, color: 'var(--muted)' }}>{fmt(porCobrar)} por cobrar</p>
          </div>

          {mesasOcupadas.length > 0 && (
            <div>
              <p style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 600, marginBottom: 8, textTransform: 'uppercase', letterSpacing: 1 }}>Mesas ocupadas</p>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                {mesasOcupadas.map(m => (
                  <span key={m.id} className={`badge ${ESTADO_MESA[m.estado]?.badge ?? ''}`}>Mesa {m.numero}</span>
                ))}
              </div>
            </div>
          )}

          <button className="btn-ghost" style={{ marginTop: 'auto' }} onClick={() => router.push('/mesas')}>
            VER MESAS
          </button>
        </div>
      </div>

      {nuevo && <NuevoPedidoModal onClose={() => setNuevo(false)} />}
    </div>
  )
}

/* ---- KPI card ---- */
function KpiCard({ label, value, icon, color }: {
  label: string; value: string; icon: ReactNode; color: string
}) {
  return (
    <div className="card" style={{ display:'flex', flexDirection:'column', gap:6 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
        <span style={{ fontSize:12, color:'var(--muted)' }}>{label}</span>
        <span style={{ color }}>{icon}</span>
      </div>
      <p style={{ fontSize:26, fontWeight:900, color:'var(--text)' }}>{value}</p>
    </div>
  )
}
