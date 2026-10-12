'use client'
import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { DollarSign, X, Check } from 'lucide-react'
import api from '@/lib/api'
import { queryClient } from '@/lib/queryClient'
import { fmt, errMsg, METODOS_PAGO } from '@/lib/utils'
import type { CuentaResponse } from '@/types'
import toast from 'react-hot-toast'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'

export default function CuentasPage() {
  const [cobrar, setCobrar] = useState<CuentaResponse | null>(null)
  const [adicion, setAdicion] = useState<CuentaResponse | null>(null)

  const { data: cuentas = [], isLoading } = useQuery<CuentaResponse[]>({
    queryKey: ['cuentas'],
    queryFn: async () => {
      const { data } = await api.get('/cuentas')
      return Array.isArray(data) ? data : data.content ?? []
    },
    refetchInterval: 20_000,
  })

  const cerrar = useMutation({
    mutationFn: (id: number) => api.patch(`/cuentas/${id}/cerrar`),
    onSuccess: () => { toast.success('Cuenta cerrada'); queryClient.invalidateQueries({ queryKey: ['cuentas'] }) },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })

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
                      <button className="btn-ghost" style={{ flex:1, justifyContent:'center', fontSize:11, padding:'7px 8px' }} onClick={() => { if (confirm('¿Cerrar cuenta?')) cerrar.mutate(c.id) }}>
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
    </div>
  )
}

function AdicionModal({ cuenta, onClose }: { cuenta: CuentaResponse; onClose: () => void }) {
  const [notas, setNotas] = useState('')

  const m = useMutation({
    mutationFn: () => {
      const codigo = `ADI-${Date.now().toString(36).toUpperCase()}${Math.random().toString(36).slice(2, 6).toUpperCase()}`
      return api.post(`/cuentas/${cuenta.id}/adiciones`, { codigo, notas: notas || null })
    },
    onSuccess: (res) => {
      toast.success(`Adición ${res.data.pedidoCodigo} creada — agrega ítems al pedido`)
      queryClient.invalidateQueries({ queryKey: ['cuentas'] })
      queryClient.invalidateQueries({ queryKey: ['pedidos'] })
      onClose()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })

  return (
    <div className="overlay">
      <div style={{ width:'100%', maxWidth:360, background:'var(--surface)', border:'1px solid var(--border)', borderRadius:20, padding:24 }}>
        <div style={{ display:'flex', justifyContent:'space-between', marginBottom:16 }}>
          <h2 style={{ fontWeight:700, fontSize:16 }}>Adición — Mesa {cuenta.numeroMesa}</h2>
          <button onClick={onClose} style={{ background:'none', border:'none', cursor:'pointer', color:'var(--muted)' }}><X size={18}/></button>
        </div>
        <p style={{ fontSize:12, color:'var(--muted)', marginBottom:14 }}>
          Se crea un pedido nuevo en borrador ligado a esta cuenta. Luego agrega los ítems desde la pantalla de Pedidos.
        </p>
        <label style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:6 }}>NOTAS (opcional)</label>
        <textarea value={notas} onChange={e => setNotas(e.target.value)} className="input" rows={2} style={{ resize:'none' }} />
        <div style={{ display:'flex', gap:8, marginTop:20 }}>
          <button className="btn-yellow" style={{ flex:1, justifyContent:'center' }} onClick={() => m.mutate()} disabled={m.isPending}>
            <Check size={14}/> {m.isPending ? 'Creando…' : 'Crear adición'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}

function CobrarModal({ cuenta, onClose }: { cuenta: CuentaResponse; onClose: () => void }) {
  const [metodo, setMetodo] = useState('EFECTIVO')
  const [monto,  setMonto]  = useState(cuenta.total?.toString() ?? '')

  const m = useMutation({
    mutationFn: () => api.post(`/cuentas/${cuenta.id}/cobros`, { pagos: [{ metodo, monto: parseFloat(monto) }], documento: null }),
    onSuccess: () => { toast.success('Pago registrado'); queryClient.invalidateQueries({ queryKey: ['cuentas'] }); queryClient.invalidateQueries({ queryKey: ['cajas'] }); onClose() },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })

  return (
    <div className="overlay">
      <div style={{ width:'100%', maxWidth:360, background:'var(--surface)', border:'1px solid var(--border)', borderRadius:20, padding:24 }}>
        <div style={{ display:'flex', justifyContent:'space-between', marginBottom:16 }}>
          <h2 style={{ fontWeight:700, fontSize:16 }}>Cobrar — Mesa {cuenta.numeroMesa}</h2>
          <button onClick={onClose} style={{ background:'none', border:'none', cursor:'pointer', color:'var(--muted)' }}><X size={18}/></button>
        </div>
        <p style={{ fontSize:32, fontWeight:900, color:'var(--yellow)', marginBottom:20 }}>{fmt(cuenta.total)}</p>
        <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
          <div>
            <label style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:6 }}>MÉTODO DE PAGO</label>
            <select value={metodo} onChange={e => setMetodo(e.target.value)} className="input">
              {METODOS_PAGO.map(x => <option key={x.value} value={x.value}>{x.label}</option>)}
            </select>
          </div>
          <div>
            <label style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:6 }}>MONTO</label>
            <input type="number" step="0.01" value={monto} onChange={e => setMonto(e.target.value)} className="input"/>
          </div>
        </div>
        <div style={{ display:'flex', gap:8, marginTop:20 }}>
          <button className="btn-yellow" style={{ flex:1, justifyContent:'center' }} onClick={() => m.mutate()} disabled={m.isPending || !monto}>
            <Check size={14}/> {m.isPending ? 'Procesando…' : 'Confirmar pago'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}
