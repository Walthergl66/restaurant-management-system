'use client'
import { useState } from 'react'
import { DollarSign } from 'lucide-react'
import { fmt } from '@/shared/lib/utils'
import { useCuentas, useCerrarCuenta } from '../hooks'
import type { CuentaResponse } from '../types'
import CobrarModal from './CobrarModal'
import AdicionModal from './AdicionModal'
import ConfirmDialog from '@/shared/ui/ConfirmDialog'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'

export default function CuentasScreen() {
  const [cobrar, setCobrar] = useState<CuentaResponse | null>(null)
  const [adicion, setAdicion] = useState<CuentaResponse | null>(null)
  const [cerrando, setCerrando] = useState<CuentaResponse | null>(null)

  const { data: cuentas = [], isLoading } = useCuentas()
  const cerrar = useCerrarCuenta()

  const abiertas = cuentas.filter(c => c.estado === 'ABIERTA')
  const cerradas = cuentas.filter(c => c.estado === 'CERRADA').slice(0, 20)

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <h1 style={{ fontSize:20, fontWeight:800 }}>Cuentas de mesa</h1>

      {/* kpis */}
      <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(160px,1fr))', gap:12 }}>
        {[
          { label:'Abiertas',       value: String(abiertas.length),                                            color:'var(--fuchsia)' },
          { label:'Cerradas',       value: String(cerradas.length),                                            color:'var(--muted)'   },
          { label:'Total abierto',  value: fmt(abiertas.reduce((s,c) => s+c.total,0)),                         color:'var(--yellow)'  },
          { label:'Total cerrado',  value: fmt(cerradas.reduce((s,c) => s+c.total,0)),                         color:'var(--success)' },
        ].map(k => (
          <div key={k.label} className="card">
            <p style={{ fontSize:11, color:'var(--muted)', marginBottom:4 }}>{k.label}</p>
            <p style={{ fontSize:20, fontWeight:800, color:k.color }}>{k.value}</p>
          </div>
        ))}
      </div>

      {/* abiertas */}
      <div className="card">
        <h2 style={{ fontWeight:700, fontSize:15, marginBottom:14 }}>Cuentas abiertas</h2>
        {isLoading
          ? <p style={{ fontSize:13, color:'var(--dim)' }}>Cargando…</p>
          : abiertas.length === 0
            ? <p style={{ fontSize:13, color:'var(--dim)' }}>Sin cuentas abiertas</p>
            : (
              <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(185px,1fr))', gap:12 }}>
                {abiertas.map(c => (
                  <div key={c.id} style={{ background:'var(--surface-2)', border:'1px solid var(--border)', borderRadius:14, padding:16, display:'flex', flexDirection:'column', gap:8 }}>
                    <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
                      <span style={{ fontWeight:700, fontSize:14 }}>Mesa {c.numeroMesa}</span>
                      <span className="badge badge-abierta">Abierta</span>
                    </div>
                    <p style={{ fontSize:24, fontWeight:900, color:'var(--yellow)' }}>{fmt(c.total)}</p>
                    <p style={{ fontSize:11, color:'var(--muted)' }}>
                      {c.pedidos?.length ?? 0} pedido(s) desde {c.createdAt ? format(new Date(c.createdAt),'HH:mm',{locale:es}) : '—'}
                    </p>
                    <button className="btn-ghost" style={{ justifyContent:'center', fontSize:11, padding:'7px 8px' }} onClick={() => setAdicion(c)}>
                      + Adición
                    </button>
                    <div style={{ display:'flex', gap:6 }}>
                      <button className="btn-fuchsia" style={{ flex:1, justifyContent:'center', fontSize:11, padding:'7px 8px' }} onClick={() => setCobrar(c)}>
                        <DollarSign size={12}/> Cobrar
                      </button>
                      <button className="btn-ghost" style={{ flex:1, justifyContent:'center', fontSize:11, padding:'7px 8px' }} onClick={() => setCerrando(c)}>
                        Cerrar
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )
        }
      </div>

      {/* historial */}
      <div className="card" style={{ padding:0, overflow:'hidden' }}>
        <div style={{ padding:'14px 20px', borderBottom:'1px solid var(--border)' }}>
          <h2 style={{ fontWeight:700, fontSize:15 }}>Historial reciente</h2>
        </div>
        {cerradas.length === 0
          ? <p style={{ padding:20, fontSize:13, color:'var(--dim)' }}>Sin historial</p>
          : (
            <table className="tbl">
              <thead><tr>{['ID','Mesa','Total','Cerrada el'].map(h => <th key={h}>{h}</th>)}</tr></thead>
              <tbody>
                {cerradas.map(c => (
                  <tr key={c.id}>
                    <td style={{ color:'var(--muted)' }}>#{c.id}</td>
                    <td style={{ fontWeight:600 }}>Mesa {c.numeroMesa}</td>
                    <td style={{ color:'var(--yellow)', fontWeight:700 }}>{fmt(c.total)}</td>
                    <td style={{ fontSize:12, color:'var(--muted)' }}>{c.updatedAt ? format(new Date(c.updatedAt),'dd/MM/yy HH:mm',{locale:es}) : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
        }
      </div>

      {cobrar && <CobrarModal cuenta={cobrar} onClose={() => setCobrar(null)} />}
      {adicion && <AdicionModal cuenta={adicion} onClose={() => setAdicion(null)} />}

      {cerrando && (
        <ConfirmDialog
          title="Cerrar cuenta"
          message={`¿Cerrar la cuenta de la mesa ${cerrando.numeroMesa}? La mesa quedará libre y la cuenta pasará al historial.`}
          confirmText="Cerrar cuenta"
          pending={cerrar.isPending}
          onConfirm={() => cerrar.mutate(cerrando.id, { onSuccess: () => setCerrando(null) })}
          onClose={() => setCerrando(null)}
        />
      )}
    </div>
  )
}
