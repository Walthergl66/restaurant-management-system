'use client'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell, PieChart, Pie, Legend } from 'recharts'
import api from '@/lib/api'
import { fmt } from '@/lib/utils'
import type { ReporteVentas, ResumenFinanzas } from '@/types'
import { format, subDays } from 'date-fns'
import { es } from 'date-fns/locale'

const COLORS = ['#F5A623','#E91E8C','#22c55e','#3b82f6','#8b5cf6','#f97316']

export default function ReportesPage() {
  const today = new Date()
  const [desde, setDesde] = useState(format(subDays(today,6),'yyyy-MM-dd'))
  const [hasta, setHasta] = useState(format(today,'yyyy-MM-dd'))

  const { data: rep, isLoading } = useQuery<ReporteVentas>({
    queryKey: ['reportes','ventas',desde,hasta],
    queryFn: async () => { const { data } = await api.get('/reportes/ventas',{ params:{ desde,hasta } }); return data },
  })

  const { data: fin } = useQuery<ResumenFinanzas>({
    queryKey: ['finanzas',desde,hasta],
    queryFn: async () => { const { data } = await api.get('/finanzas/resumen',{ params:{ desde,hasta } }); return data },
  })

  const RANGOS = [{ label:'Hoy',d:0 },{ label:'7 días',d:6 },{ label:'30 días',d:29 }]

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:10 }}>
        <h1 style={{ fontSize:20, fontWeight:800 }}>Reportes</h1>
        <div style={{ display:'flex', gap:6, flexWrap:'wrap', alignItems:'center' }}>
          {RANGOS.map(({ label, d }) => (
            <button
              key={label}
              onClick={() => { setDesde(format(subDays(today,d),'yyyy-MM-dd')); setHasta(format(today,'yyyy-MM-dd')) }}
              style={{ padding:'7px 14px', borderRadius:10, fontSize:12, fontWeight:600, cursor:'pointer', border:'none',
                background: desde===format(subDays(today,d),'yyyy-MM-dd') && hasta===format(today,'yyyy-MM-dd') ? 'var(--yellow)' : 'var(--surface)',
                color:      desde===format(subDays(today,d),'yyyy-MM-dd') && hasta===format(today,'yyyy-MM-dd') ? '#000' : 'var(--muted)',
                outline: !(desde===format(subDays(today,d),'yyyy-MM-dd') && hasta===format(today,'yyyy-MM-dd')) ? '1px solid var(--border)' : 'none',
              }}
            >{label}</button>
          ))}
          <input type="date" value={desde} onChange={e => setDesde(e.target.value)} className="input" style={{ width:140, padding:'7px 10px', fontSize:12 }}/>
          <span style={{ fontSize:12, color:'var(--muted)' }}>a</span>
          <input type="date" value={hasta} onChange={e => setHasta(e.target.value)} className="input" style={{ width:140, padding:'7px 10px', fontSize:12 }}/>
        </div>
      </div>

      {isLoading
        ? <p style={{ color:'var(--dim)', textAlign:'center', padding:32 }}>Cargando…</p>
        : <>
            {/* kpis */}
            <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(170px,1fr))', gap:12 }}>
              {[
                { label:'Total ventas',   value: fmt(rep?.totalVentas),              color:'var(--yellow)'  },
                { label:'Total pedidos',  value: String(rep?.totalPedidos??0),        color:'var(--fuchsia)' },
                { label:'Ticket promedio',value: fmt(rep?.ticketPromedio),            color:'var(--yellow)'  },
                { label:'Resultado neto', value: fmt(fin?.resultado),                 color:(fin?.resultado??0)>=0?'var(--success)':'var(--error)' },
              ].map(k => (
                <div key={k.label} className="card">
                  <p style={{ fontSize:11, color:'var(--muted)', marginBottom:4 }}>{k.label}</p>
                  <p style={{ fontSize:22, fontWeight:900, color:k.color }}>{k.value}</p>
                </div>
              ))}
            </div>

            {/* charts */}
            <div style={{ display:'grid', gridTemplateColumns:'1fr 280px', gap:16 }}>
              <div className="card">
                <h2 style={{ fontWeight:700, fontSize:15, marginBottom:14 }}>Ventas por día</h2>
                {!rep?.porDia?.length
                  ? <p style={{ fontSize:13, color:'var(--dim)' }}>Sin datos</p>
                  : (
                    <ResponsiveContainer width="100%" height={220}>
                      <BarChart data={rep.porDia} barSize={22} margin={{ top:4,right:4,bottom:0,left:-20 }}>
                        <XAxis dataKey="fecha" tickFormatter={v => format(new Date(v+'T00:00:00'),'dd/MM',{locale:es})} tick={{ fill:'var(--muted)',fontSize:11 }} axisLine={false} tickLine={false}/>
                        <YAxis tick={{ fill:'var(--muted)',fontSize:11 }} axisLine={false} tickLine={false}/>
                        <Tooltip contentStyle={{ background:'#1a1a1a',border:'1px solid #2a2a2a',borderRadius:8,color:'#fff',fontSize:12 }} formatter={(v) => [fmt(Number(v)),'Ventas']}/>
                        <Bar dataKey="monto" radius={[5,5,0,0]}>
                          {rep.porDia.map((_,i) => <Cell key={i} fill={i%2===0?'var(--yellow)':'var(--fuchsia)'}/>)}
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  )
                }
              </div>

              <div className="card">
                <h2 style={{ fontWeight:700, fontSize:15, marginBottom:14 }}>Por método</h2>
                {!rep?.porMetodo?.length
                  ? <p style={{ fontSize:13, color:'var(--dim)' }}>Sin datos</p>
                  : (
                    <ResponsiveContainer width="100%" height={220}>
                      <PieChart>
                        <Pie data={rep.porMetodo.map(m => ({ name:m.metodo, value:m.monto }))} cx="50%" cy="50%" innerRadius={50} outerRadius={80} dataKey="value" paddingAngle={3}>
                          {rep.porMetodo.map((_,i) => <Cell key={i} fill={COLORS[i%COLORS.length]}/>)}
                        </Pie>
                        <Tooltip contentStyle={{ background:'#1a1a1a',border:'1px solid #2a2a2a',borderRadius:8,color:'#fff',fontSize:12 }} formatter={(v) => [fmt(Number(v))]}/>
                        <Legend wrapperStyle={{ fontSize:11, color:'var(--muted)' }}/>
                      </PieChart>
                    </ResponsiveContainer>
                  )
                }
              </div>
            </div>

            {/* top productos */}
            <div className="card" style={{ padding:0, overflow:'hidden' }}>
              <div style={{ padding:'14px 20px', borderBottom:'1px solid var(--border)' }}>
                <h2 style={{ fontWeight:700, fontSize:15 }}>Top productos del período</h2>
              </div>
              {!rep?.topProductos?.length
                ? <p style={{ padding:20, fontSize:13, color:'var(--dim)' }}>Sin datos</p>
                : (
                  <table className="tbl">
                    <thead><tr>{['#','Producto','Unidades','Total'].map(h => <th key={h}>{h}</th>)}</tr></thead>
                    <tbody>
                      {rep.topProductos.map((p,i) => (
                        <tr key={i}>
                          <td style={{ fontWeight:800, fontSize:12, color:i<3?'var(--yellow)':'var(--muted)' }}>{String(i+1).padStart(2,'0')}</td>
                          <td style={{ fontWeight:600 }}>{p.productoNombre}</td>
                          <td style={{ color:'var(--muted)' }}>{p.cantidad} uds.</td>
                          <td style={{ color:'var(--yellow)', fontWeight:700 }}>{fmt(p.monto)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )
              }
            </div>

            {/* finanzas */}
            {fin && (
              <div className="card">
                <h2 style={{ fontWeight:700, fontSize:15, marginBottom:14 }}>Resumen financiero</h2>
                <div style={{ display:'grid', gridTemplateColumns:'repeat(3,1fr)', gap:16, textAlign:'center' }}>
                  {[
                    { label:'Ingresos', value:fmt(fin.ingresos), color:'var(--success)' },
                    { label:'Egresos',  value:fmt(fin.egresos),  color:'var(--error)'   },
                    { label:'Resultado',value:fmt(fin.resultado), color:(fin.resultado??0)>=0?'var(--success)':'var(--error)' },
                  ].map(k => (
                    <div key={k.label}>
                      <p style={{ fontSize:11, color:'var(--muted)', marginBottom:4 }}>{k.label}</p>
                      <p style={{ fontSize:22, fontWeight:900, color:k.color }}>{k.value}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </>
      }
    </div>
  )
}
