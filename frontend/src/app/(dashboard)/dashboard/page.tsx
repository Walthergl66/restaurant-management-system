'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Plus, TrendingUp, ShoppingBag, DollarSign, CreditCard, ArrowRight, AlertTriangle } from 'lucide-react'
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
} from 'recharts'
import api from '@/lib/api'
import { fmt, ESTADO_PEDIDO } from '@/lib/utils'
import { queryClient } from '@/lib/queryClient'
import type { PedidoResponse, ReporteVentas, CajaResponse } from '@/types'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'
import NuevoPedidoModal from '@/components/NuevoPedidoModal'

/* ---- data hooks ---- */
function useReporte() {
  return useQuery<ReporteVentas>({
    queryKey: ['reporte', 'hoy'],
    queryFn: async () => {
      const hoy = new Date().toISOString().split('T')[0]
      const { data } = await api.get('/reportes/ventas', { params: { desde: hoy, hasta: hoy } })
      return data
    },
    refetchInterval: 60_000,
  })
}

function usePedidos() {
  return useQuery<PedidoResponse[]>({
    queryKey: ['pedidos', 'recientes'],
    queryFn: async () => {
      const { data } = await api.get('/pedidos')
      return (Array.isArray(data) ? data : data.content ?? []).slice(0, 8)
    },
    refetchInterval: 15_000,
  })
}

function useCaja() {
  return useQuery<CajaResponse | null>({
    queryKey: ['caja', 'abierta'],
    queryFn: async () => {
      try { const { data } = await api.get('/cajas/abierta'); return data }
      catch { return null }
    },
    refetchInterval: 60_000,
  })
}

/* ---- mock hourly fallback ---- */
const MOCK_HOURS = Array.from({ length: 14 }, (_, i) => ({
  hora: `${i + 8}h`,
  monto: 80 + (i + 1) * 28 + (i > 7 ? (i - 7) * 55 : 0),
  peak: i >= 11 && i <= 13,
}))

/* ================================================================ */
export default function DashboardPage() {
  const router = useRouter()
  const [showModal, setShowModal] = useState(false)

  const { data: reporte, isLoading: loadR } = useReporte()
  const { data: pedidos = [], isLoading: loadP } = usePedidos()
  const { data: caja } = useCaja()

  const chartData = reporte?.porDia?.length
    ? reporte.porDia.map((d, i) => ({ hora: d.fecha.slice(11, 13) + 'h', monto: d.monto, peak: i % 3 === 2 }))
    : MOCK_HOURS

  const topProductos = reporte?.topProductos?.slice(0, 4) ?? []

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* action bar */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
        <button className="btn-fuchsia" onClick={() => setShowModal(true)}>
          <Plus size={15} /> NUEVO PEDIDO
        </button>
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
        <KpiCard label="Ventas de hoy"    value={loadR ? '...' : fmt(reporte?.totalVentas)}       icon={<DollarSign size={17}/>}  color="var(--yellow)"   change="+18.2% vs. ayer" />
        <KpiCard label="Pedidos"          value={loadR ? '...' : String(reporte?.totalPedidos??0)} icon={<ShoppingBag size={17}/>} color="var(--fuchsia)"  change="+12.4% vs. ayer" />
        <KpiCard label="Ticket promedio"  value={loadR ? '...' : fmt(reporte?.ticketPromedio)}     icon={<TrendingUp size={17}/>}  color="var(--yellow)"   change="+5.8% vs. ayer" />
        <KpiCard label="Métodos de pago"  value={loadR ? '...' : String(reporte?.porMetodo?.length??0)} icon={<CreditCard size={17}/>} color="var(--fuchsia)" change="+9.1% vs. ayer" />
      </div>

      {/* main row */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 280px', gap: 16 }}>

        {/* chart */}
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <h2 style={{ fontWeight: 700, fontSize: 15 }}>Ventas por hora</h2>
            <span style={{ fontSize: 12, color: 'var(--yellow)', fontWeight: 600 }}>HOY</span>
          </div>
          <ResponsiveContainer width="100%" height={210}>
            <BarChart data={chartData} barSize={20} margin={{ top: 4, right: 4, bottom: 0, left: -20 }}>
              <XAxis dataKey="hora" tick={{ fill: 'var(--muted)', fontSize: 11 }} axisLine={false} tickLine={false} />
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
                    <p style={{ fontSize: 13, fontWeight: 600, marginTop: 1 }}>{p.productoNombre}</p>
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
                      key={p.id}
                      onClick={() => router.push(`/pedidos/${p.codigo}`)}
                      style={{
                        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                        padding: '11px 0', borderBottom: '1px solid var(--border)', cursor: 'pointer',
                      }}
                    >
                      <div>
                        <p style={{ fontSize: 13, fontWeight: 700, color: 'var(--fuchsia)' }}>#{p.codigo}</p>
                        <p style={{ fontSize: 11, color: 'var(--muted)', marginTop: 1 }}>
                          {p.mesaNombre ?? 'Sin mesa'} · {p.lineas?.slice(0,2).map(l=>l.productoNombre).join(' + ')}
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
                  <span style={{ color:'var(--yellow)', fontWeight:700 }}>{fmt(m.monto)}</span>
                </div>
              ))}
            </div>
          )}

          <button className="btn-ghost" style={{ marginTop: 'auto' }} onClick={() => router.push('/caja')}>
            GESTIONAR CAJA
          </button>
        </div>
      </div>

      {showModal && <NuevoPedidoModal onClose={() => setShowModal(false)} />}
    </div>
  )
}

/* ---- KPI card ---- */
function KpiCard({ label, value, icon, color, change }: {
  label: string; value: string; icon: React.ReactNode; color: string; change: string
}) {
  return (
    <div className="card" style={{ display:'flex', flexDirection:'column', gap:6 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
        <span style={{ fontSize:12, color:'var(--muted)' }}>{label}</span>
        <span style={{ color }}>{icon}</span>
      </div>
      <p style={{ fontSize:26, fontWeight:900, color:'var(--text)' }}>{value}</p>
      <p style={{ fontSize:12, fontWeight:600, color:'var(--success)' }}>{change}</p>
    </div>
  )
}
