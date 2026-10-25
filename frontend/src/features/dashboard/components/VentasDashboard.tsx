'use client'
import { useRouter } from 'next/navigation'
import { TrendingUp, ShoppingBag, DollarSign, CreditCard, ArrowRight, AlertTriangle } from 'lucide-react'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
} from 'recharts'
import type { ReactNode } from 'react'
import { fmt, ESTADO_PEDIDO } from '@/shared/lib/utils'
import { useReporte, useReporteAyer, useReporteSemana, usePedidos, useCaja } from '../hooks'
import QueryBoundary, { hasQueryIssue } from '@/shared/ui/QueryBoundary'

/* etiqueta dd/MM a partir de la fecha (ISO o yyyy-MM-dd) que devuelve el backend */
function etiquetaDia(fecha: string): string {
  const d = new Date(String(fecha).length > 10 ? fecha : `${fecha}T00:00:00`)
  return Number.isNaN(d.getTime()) ? String(fecha) : `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`
}

/* variación porcentual real de un valor contra el mismo de ayer; null si no hay base */
function variacion(actual: number, previo: number): { text: string; up: boolean } | null {
  if (!previo) return null
  const pct = ((actual - previo) / previo) * 100
  return { text: `${pct >= 0 ? '+' : ''}${pct.toFixed(1)}% vs. ayer`, up: pct >= 0 }
}

/* ================================================================ */
export default function VentasDashboard() {
  const router = useRouter()
  const reporteQuery = useReporte()
  const ayerQuery = useReporteAyer()
  const semanaQuery = useReporteSemana()
  const pedidosQuery = usePedidos()
  const cajaQuery = useCaja()
  const { data: reporte, isLoading: loadR } = reporteQuery
  const { data: ayer, isLoading: loadA } = ayerQuery
  const { data: semana, isLoading: loadSemana } = semanaQuery
  const { data: pedidos = [], isLoading: loadP } = pedidosQuery
  const { data: caja } = cajaQuery

  const queries = [reporteQuery, ayerQuery, semanaQuery, pedidosQuery, cajaQuery]
  if (hasQueryIssue(queries)) return <QueryBoundary queries={queries} label="el resumen de ventas" />

  const porDia = semana?.porDia ?? []
  const maxMonto = porDia.reduce((max, d) => Math.max(max, d.total), 0)
  const chartData = porDia.map(d => ({
    fecha: etiquetaDia(d.fecha),
    monto: d.total,
    peak: maxMonto > 0 && d.total === maxMonto,
  }))

  const topProductos = reporte?.porProducto?.slice(0, 4) ?? []
  const ticketPromedio = reporte && reporte.numeroVentas > 0 ? reporte.total / reporte.numeroVentas : 0
  const ticketAyer = ayer && ayer.numeroVentas > 0 ? ayer.total / ayer.numeroVentas : 0

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* action bar */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
        {caja && (
          <div style={{
            display: 'flex', alignItems: 'center', gap: 8, padding: '8px 14px',
            background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 12, fontSize: 13,
          }}>
            <span style={{ width: 8, height: 8, borderRadius: '50%', background: 'var(--success)' }} />
            <span style={{ color: 'var(--muted)' }}>
              Caja abierta · <span style={{ color: 'var(--yellow)', fontWeight: 700 }}>{fmt(caja.aperturaInicial)}</span>
            </span>
          </div>
        )}
      </div>

      {/* KPIs */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(190px,1fr))', gap: 14 }}>
        <KpiCard label="Ventas de hoy"    value={loadR ? '...' : fmt(reporte?.total)}       icon={<DollarSign size={17}/>}  color="var(--yellow)"   change={loadR || loadA ? undefined : variacion(reporte?.total ?? 0, ayer?.total ?? 0)} />
        <KpiCard label="Pedidos"          value={loadR ? '...' : String(reporte?.numeroVentas??0)} icon={<ShoppingBag size={17}/>} color="var(--fuchsia)"  change={loadR || loadA ? undefined : variacion(reporte?.numeroVentas ?? 0, ayer?.numeroVentas ?? 0)} />
        <KpiCard label="Ticket promedio"  value={loadR ? '...' : fmt(ticketPromedio)}     icon={<TrendingUp size={17}/>}  color="var(--yellow)"   change={loadR || loadA ? undefined : variacion(ticketPromedio, ticketAyer)} />
        <KpiCard label="Métodos de pago"  value={loadR ? '...' : String(reporte?.porMetodo?.length??0)} icon={<CreditCard size={17}/>} color="var(--fuchsia)" change={loadR || loadA ? undefined : variacion(reporte?.porMetodo?.length ?? 0, ayer?.porMetodo?.length ?? 0)} />
      </div>

      {/* main row */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 280px', gap: 16 }}>

        {/* chart */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h2 style={{ fontWeight: 700, fontSize: 15 }}>Ventas de los últimos 7 días</h2>
            <span style={{ fontSize: 12, color: 'var(--yellow)', fontWeight: 600 }}>7 DÍAS</span>
          </div>
          {loadSemana
            ? <p style={{ fontSize: 13, color: 'var(--dim)', padding: '80px 0', textAlign: 'center' }}>Cargando…</p>
            : chartData.length === 0
              ? <p style={{ fontSize: 13, color: 'var(--dim)', padding: '80px 0', textAlign: 'center' }}>Sin datos aún</p>
              : <ResponsiveContainer width="100%" height={210}>
                  <BarChart data={chartData} barSize={20} margin={{ top: 4, right: 4, bottom: 0, left: -20 }}>
                    <XAxis dataKey="fecha" tick={{ fill: 'var(--muted)', fontSize: 11 }} axisLine={false} tickLine={false} />
                    <YAxis tick={{ fill: 'var(--muted)', fontSize: 11 }} axisLine={false} tickLine={false} />
                    <Tooltip
                      contentStyle={{ background: '#1a1a1a', border: '1px solid #2a2a2a', borderRadius: 8, color: '#fff', fontSize: 12 }}
                      cursor={{ fill: 'rgba(255,255,255,.03)' }}
                      formatter={(v) => [fmt(Number(v)), 'Ventas']}
                    />
                    <Bar dataKey="monto" radius={[5,5,0,0]}>
                      {chartData.map((d, i) => (
                        <Cell key={i} fill={d.peak ? 'var(--fuchsia)' : 'var(--yellow)'} />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
          }
        </div>

        {/* top productos */}
        <div className="card">
          <h2 style={{ fontWeight: 700, fontSize: 15, marginBottom: 16 }}>Productos más vendidos</h2>
          {topProductos.length === 0
            ? <p style={{ fontSize: 13, color: 'var(--dim)' }}>Sin datos aún</p>
            : <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
                {topProductos.map((p, i) => (
                  <div key={i}>
                    <span style={{ fontSize: 11, fontWeight: 800, color: i%2===0?'var(--fuchsia)':'var(--yellow)' }}>
                      {String(i+1).padStart(2,'0')}
                    </span>
                    <p style={{ fontSize: 13, fontWeight: 600, marginTop: 1 }}>{p.nombre}</p>
                    <p style={{ fontSize: 11, color: 'var(--muted)' }}>{p.cantidad} uds.</p>
                    <p style={{ fontSize: 13, fontWeight: 800, color: i%2===0?'var(--fuchsia)':'var(--yellow)' }}>{fmt(p.monto)}</p>
                  </div>
                ))}
              </div>
          }
        </div>
      </div>

      {/* bottom row */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 280px', gap: 16 }}>

        {/* pedidos recientes */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
            <h2 style={{ fontWeight: 700, fontSize: 15 }}>Pedidos recientes</h2>
            <button onClick={() => router.push('/pedidos')} style={{
              display: 'flex', alignItems: 'center', gap: 4, fontSize: 12,
              fontWeight: 600, color: 'var(--yellow)', background: 'none', border: 'none', cursor: 'pointer',
            }}>
              Ver todos <ArrowRight size={13} />
            </button>
          </div>
          {loadP
            ? <p style={{ fontSize: 13, color: 'var(--dim)' }}>Cargando...</p>
            : pedidos.length === 0
              ? <p style={{ fontSize: 13, color: 'var(--dim)' }}>No hay pedidos aún</p>
              : <div style={{ display: 'flex', flexDirection: 'column' }}>
                  {pedidos.map((p) => (
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

        {/* estado sistema */}
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <h2 style={{ fontWeight: 700, fontSize: 15 }}>Estado del sistema</h2>

          {/* caja */}
          <div style={{ background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: 12, padding: 14 }}>
            <p style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 600, marginBottom: 8, textTransform: 'uppercase', letterSpacing: 1 }}>Caja actual</p>
            {caja
              ? <>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, marginBottom: 4 }}>
                    <span style={{ color: 'var(--muted)' }}>Apertura</span>
                    <span style={{ color: 'var(--yellow)', fontWeight: 700 }}>{fmt(caja.aperturaInicial)}</span>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13 }}>
                    <span style={{ color: 'var(--muted)' }}>Estado</span>
                    <span className="badge badge-abierta">ABIERTA</span>
                  </div>
                </>
              : <div style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, color: 'var(--warning)' }}>
                  <AlertTriangle size={14} /> Sin caja abierta
                </div>
            }
          </div>

          {/* métodos */}
          {reporte?.porMetodo && reporte.porMetodo.length > 0 && (
            <div>
              <p style={{ fontSize: 11, color: 'var(--muted)', fontWeight: 600, marginBottom: 8, textTransform: 'uppercase', letterSpacing: 1 }}>Por método</p>
              {reporte.porMetodo.slice(0,4).map(m => (
                <div key={m.metodo} style={{ display:'flex', justifyContent:'space-between', fontSize:13, marginBottom:4 }}>
                  <span style={{ color:'var(--muted)' }}>{m.metodo}</span>
                  <span style={{ color:'var(--yellow)', fontWeight:700 }}>{fmt(m.total)}</span>
                </div>
              ))}
            </div>
          )}

          <button className="btn-ghost" style={{ marginTop: 'auto' }} onClick={() => router.push('/caja')}>
            GESTIONAR CAJA
          </button>
        </div>
      </div>
    </div>
  )
}

/* ---- KPI card ---- */
function KpiCard({ label, value, icon, color, change }: {
  label: string; value: string; icon: ReactNode; color: string; change?: { text: string; up: boolean } | null
}) {
  return (
    <div className="card" style={{ display:'flex', flexDirection:'column', gap:6 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
        <span style={{ fontSize:12, color:'var(--muted)' }}>{label}</span>
        <span style={{ color }}>{icon}</span>
      </div>
      <p style={{ fontSize:26, fontWeight:900, color:'var(--text)' }}>{value}</p>
      {change
        ? <p style={{ fontSize:12, fontWeight:600, color: change.up ? 'var(--success)' : 'var(--error)' }}>{change.text}</p>
        : <p style={{ fontSize:12, fontWeight:600, color:'var(--muted)' }}>Sin datos previos</p>
      }
    </div>
  )
}
