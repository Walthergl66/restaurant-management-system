'use client'
import Link from 'next/link'
import { ArrowRight, Sandwich, Store, BarChart3, Star, Coins, ShoppingBag, Ticket, CreditCard } from 'lucide-react'
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
      <section className={styles.operationGrid} aria-label="Operación">
        <div className={styles.panel}>
          <div className={styles.panelHeader}><h2>Pedidos recientes</h2><Link href="/pedidos">Ver pedidos <ArrowRight size={17} /></Link></div>
          {loadP ? <p className={styles.empty}>Cargando pedidos…</p> : pedidos.length === 0 ? <div className={styles.emptyState}><Sandwich size={76}/><strong>Aún no hay pedidos recientes</strong><span>Los nuevos pedidos aparecerán aquí.</span></div> : <div className={styles.tickets}>{pedidos.map(p => <Link className={styles.ticket} href={`/pedidos/${encodeURIComponent(p.codigo)}`} key={p.codigo}><strong>#{p.codigo}</strong><span>{ESTADO_PEDIDO[p.estado]?.label ?? p.estado}</span><span>{p.mesaId != null ? `Mesa ${p.mesaId}` : 'Sin mesa'} · {p.lineas?.slice(0,2).map(l => `${l.cantidad} × ${l.nombreProducto}`).join(' · ') || 'Sin productos'}</span><b>{fmt(p.total)}</b></Link>)}</div>}
        </div>
        <div className={styles.panel}>
          <div className={styles.panelHeader}><h2>Caja del turno</h2><Link href="/caja">Gestionar caja <ArrowRight size={17}/></Link></div>
          <div className={styles.emptyState}><Store size={76} aria-hidden="true"/>{loadCaja ? <strong>Consultando…</strong> : caja ? <><strong className={styles.open}>Abierta</strong><span>Apertura inicial {fmt(caja.aperturaInicial)}</span></> : <><strong className={styles.closed}>Sin abrir</strong><span>La caja del turno aún no ha sido abierta.</span></>}</div>
        </div>
      </section>
      <section className={styles.metrics} aria-label="Ventas del día">
        <div><Coins aria-hidden="true"/><span>Ventas de hoy</span><strong>{loadR ? '…' : fmt(reporte?.total)}</strong></div><div><ShoppingBag aria-hidden="true"/><span>Ventas realizadas</span><strong>{loadR ? '…' : reporte?.numeroVentas ?? 0}</strong></div><div><Ticket aria-hidden="true"/><span>Ticket promedio</span><strong>{loadR ? '…' : fmt(ticketPromedio)}</strong></div><div><CreditCard aria-hidden="true"/><span>Métodos de pago</span><strong>{loadR ? '…' : reporte?.porMetodo?.length ?? 0}</strong></div>
      </section>
      <section className={styles.analytics} aria-label="Estadísticas">
        <div className={styles.panel}><div className={styles.panelHeader}><h2>Ventas de los últimos 7 días</h2></div>{loadSemana ? <p className={styles.empty}>Cargando…</p> : chartData.length === 0 ? <div className={styles.emptyState}><BarChart3 size={64}/><strong>Aún no hay ventas registradas</strong><span>Aquí se mostrará el comportamiento de ventas de los últimos 7 días.</span></div> : <ResponsiveContainer width="100%" height={200}><BarChart data={chartData} barSize={20} margin={{ top: 12, right: 4, bottom: 0, left: -20 }}><XAxis dataKey="fecha" tick={{ fill: '#655b52', fontSize: 11 }} axisLine={false} tickLine={false}/><YAxis tick={{ fill: '#655b52', fontSize: 11 }} axisLine={false} tickLine={false}/><Tooltip formatter={(v) => [fmt(Number(v)), 'Ventas']}/><Bar dataKey="monto" radius={[5,5,0,0]}>{chartData.map((d,i) => <Cell key={i} fill={d.peak ? '#e50069' : '#f5a000'}/>)}</Bar></BarChart></ResponsiveContainer>}</div>
        <div className={styles.panel}><div className={styles.panelHeader}><h2>Los favoritos</h2></div>{loadR ? <p className={styles.empty}>Cargando…</p> : topProductos.length === 0 ? <div className={styles.emptyState}><Star size={64}/><strong>Aún no hay productos favoritos</strong><span>Aquí se mostrarán los productos más vendidos de hoy.</span></div> : <div className={styles.products}>{topProductos.map((p,i) => <div key={`${p.nombre}-${i}`}><span>{i+1}</span><strong>{p.nombre}</strong><span>{p.cantidad} uds.</span><b>{fmt(p.monto)}</b></div>)}</div>}</div>
      </section>
    </div>
  )
}
