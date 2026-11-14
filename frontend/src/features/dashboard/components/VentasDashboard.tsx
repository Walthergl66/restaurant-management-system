'use client'
import Link from 'next/link'
import { ArrowRight, ChefHat, ReceiptText, Wallet, TrendingUp } from 'lucide-react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts'
import { fmt, ESTADO_PEDIDO } from '@/shared/lib/utils'
import { useReporte, useReporteSemana, usePedidos, useCaja } from '../hooks'
import QueryBoundary, { hasQueryIssue } from '@/shared/ui/QueryBoundary'
import styles from './VentasDashboard.module.css'

function etiquetaDia(fecha: string): string {
  const d = new Date(String(fecha).length > 10 ? fecha : `${fecha}T00:00:00`)
  return Number.isNaN(d.getTime()) ? String(fecha) : `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`
}

export default function VentasDashboard() {
  const reporteQuery = useReporte()
  const semanaQuery = useReporteSemana()
  const pedidosQuery = usePedidos()
  const cajaQuery = useCaja()
  const queries = [reporteQuery, semanaQuery, pedidosQuery, cajaQuery]
  const { data: reporte, isLoading: loadR } = reporteQuery
  const { data: semana, isLoading: loadSemana } = semanaQuery
  const { data: pedidos = [], isLoading: loadP } = pedidosQuery
  const { data: caja, isLoading: loadCaja } = cajaQuery
  if (hasQueryIssue(queries)) return <QueryBoundary queries={queries} label="el resumen de ventas" />

  const porDia = semana?.porDia ?? []
  const maxMonto = porDia.reduce((max, d) => Math.max(max, d.total), 0)
  const chartData = porDia.map(d => ({ fecha: etiquetaDia(d.fecha), monto: d.total, peak: maxMonto > 0 && d.total === maxMonto }))
  const topProductos = reporte?.porProducto?.slice(0, 4) ?? []
  const ticketPromedio = reporte && reporte.numeroVentas > 0 ? reporte.total / reporte.numeroVentas : 0

  return (
    <div className={styles.dashboard}>
      <section className={styles.hero} aria-labelledby="turno-title">
        <div className={styles.heroCopy}>
          <span className={styles.heroTag}><span className={styles.dot} /> Estación Burger</span>
          <h2 id="turno-title">Tu estación,<br /><span>todo a la vista.</span></h2>
          <p>Consulta los pedidos, la caja y las ventas del restaurante.</p>
          <Link href="/pedidos" className={styles.heroAction}>Ver pedidos <ArrowRight size={17} /></Link>
        </div>
        <div className={styles.heroMark} aria-hidden="true"><ChefHat size={70} strokeWidth={1.1} /><span>ESTACIÓN<br />BURGER</span></div>
      </section>

      <section className={styles.operation} aria-labelledby="operation-title">
        <div className={styles.sectionHeading}><div><span className={styles.sectionNote}>Lo que pasa ahora</span><h2 id="operation-title">En la estación</h2></div><Link href="/pedidos" className={styles.textLink}>Ver pedidos <ArrowRight size={15} /></Link></div>
        <div className={styles.operationGrid}>
          <div className={styles.orderBoard}>
            <div className={styles.boardHeading}><span><ReceiptText size={18} /> Pedidos recientes</span><small>Últimos {pedidos.length || '—'}</small></div>
            {loadP ? <p className={styles.empty}>Cargando pedidos…</p> : pedidos.length === 0
              ? <div className={styles.emptyState}><ReceiptText size={28} /><strong>Aún no hay pedidos</strong><span>Los próximos pedidos aparecerán aquí.</span></div>
              : <div className={styles.tickets}>{pedidos.map(p => (
                  <Link href={`/pedidos/${encodeURIComponent(p.codigo)}`} key={p.codigo} className={styles.ticket}>
                    <div className={styles.ticketTop}><strong>#{p.codigo}</strong><span className={`badge ${ESTADO_PEDIDO[p.estado]?.badge ?? ''}`}>{ESTADO_PEDIDO[p.estado]?.label ?? p.estado}</span></div>
                    <span className={styles.ticketLocation}>{p.mesaId != null ? `Mesa ${p.mesaId}` : 'Sin mesa'}</span>
                    <span className={styles.ticketItems}>{p.lineas?.slice(0, 2).map(l => `${l.cantidad} × ${l.nombreProducto}`).join(' · ') || 'Sin productos'}</span>
                    <span className={styles.ticketTotal}>{fmt(p.total)} <ArrowRight size={15} /></span>
                  </Link>
                ))}</div>}
          </div>
          <div className={styles.cashCard}>
            <div className={styles.cashIcon}><Wallet size={20} /></div><span className={styles.cashLabel}>Caja del turno</span>
            {loadCaja ? <strong className={styles.cashStatus}>Consultando…</strong> : caja ? <><strong className={styles.cashStatus}>Abierta</strong><p>Apertura inicial <b>{fmt(caja.aperturaInicial)}</b></p></> : <><strong className={styles.cashStatus}>Sin abrir</strong><p>Abre una caja para iniciar los cobros.</p></>}
            <Link href="/caja" className={styles.cashLink}>Gestionar caja <ArrowRight size={16} /></Link>
          </div>
        </div>
      </section>

      <section className={styles.sales} aria-labelledby="sales-title">
        <div className={styles.sectionHeading}><div><span className={styles.sectionNote}>El resultado del día</span><h2 id="sales-title">Así van las ventas</h2></div><TrendingUp size={22} className={styles.salesIcon} /></div>
        <div className={styles.metrics}>
          <div><span>Ventas de hoy</span><strong>{loadR ? '…' : fmt(reporte?.total)}</strong></div>
          <div><span>Ventas realizadas</span><strong>{loadR ? '…' : reporte?.numeroVentas ?? 0}</strong></div>
          <div><span>Ticket promedio</span><strong>{loadR ? '…' : fmt(ticketPromedio)}</strong></div>
          <div><span>Métodos de pago</span><strong>{loadR ? '…' : reporte?.porMetodo?.length ?? 0}</strong></div>
        </div>
        <div className={styles.analytics}>
          <div className={styles.chart}><h3>Ventas de los últimos 7 días</h3>
            {loadSemana ? <p className={styles.empty}>Cargando…</p> : chartData.length === 0 ? <p className={styles.empty}>Sin datos aún</p> : <ResponsiveContainer width="100%" height={200}><BarChart data={chartData} barSize={20} margin={{ top: 4, right: 4, bottom: 0, left: -20 }}><XAxis dataKey="fecha" tick={{ fill: '#aaa', fontSize: 11 }} axisLine={false} tickLine={false} /><YAxis tick={{ fill: '#aaa', fontSize: 11 }} axisLine={false} tickLine={false} /><Tooltip contentStyle={{ background: '#1a1a1a', border: '1px solid #444', borderRadius: 8, color: '#fff', fontSize: 12 }} formatter={(v) => [fmt(Number(v)), 'Ventas']} /><Bar dataKey="monto" radius={[5, 5, 0, 0]}>{chartData.map((d, i) => <Cell key={i} fill={d.peak ? '#e91e8c' : '#f5a623'} />)}</Bar></BarChart></ResponsiveContainer>}
          </div>
          <div className={styles.topProducts}><h3>Los favoritos</h3><p>Productos más vendidos hoy</p>{loadR ? <span className={styles.empty}>Cargando…</span> : topProductos.length === 0 ? <span className={styles.empty}>Sin ventas registradas aún.</span> : topProductos.map((p, i) => <div className={styles.product} key={`${p.nombre}-${i}`}><span>{String(i + 1).padStart(2, '0')}</span><div><strong>{p.nombre}</strong><small>{p.cantidad} uds.</small></div><b>{fmt(p.monto)}</b></div>)}</div>
        </div>
      </section>
    </div>
  )
}
