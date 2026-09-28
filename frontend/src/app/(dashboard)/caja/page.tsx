'use client'
import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { Unlock, Lock, Plus, TrendingUp, TrendingDown, X, Check } from 'lucide-react'
import api from '@/lib/api'
import { queryClient } from '@/lib/queryClient'
import { fmt } from '@/lib/utils'
import type { CajaResponse, MovimientoResponse } from '@/types'
import toast from 'react-hot-toast'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'

export default function CajaPage() {
  const [showApertura, setShowApertura] = useState(false)
  const [showCierre,   setShowCierre]   = useState(false)
  const [showEgreso,   setShowEgreso]   = useState(false)

  const { data: caja, isLoading } = useQuery<CajaResponse | null>({
    queryKey: ['caja', 'abierta'],
    queryFn: async () => {
      try { const { data } = await api.get('/cajas/abierta'); return data }
      catch { return null }
    },
    refetchInterval: 30_000,
  })

  const { data: historial = [] } = useQuery<CajaResponse[]>({
    queryKey: ['cajas'],
    queryFn: async () => {
      const { data } = await api.get('/cajas')
      return Array.isArray(data) ? data.slice(0, 15) : []
    },
  })

  const { data: movimientos = [] } = useQuery<MovimientoResponse[]>({
    queryKey: ['caja', 'movimientos', caja?.id],
    queryFn: async () => {
      if (!caja?.id) return []
      const { data } = await api.get(`/cajas/${caja.id}/movimientos`)
      return data
    },
    enabled: !!caja?.id,
  })

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:20 }}>
      <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', flexWrap:'wrap', gap:10 }}>
        <h1 style={{ fontSize:20, fontWeight:800 }}>Caja</h1>
        <div style={{ display:'flex', gap:8 }}>
          {!caja && !isLoading && (
            <button className="btn-yellow" onClick={() => setShowApertura(true)}><Unlock size={15}/> Abrir caja</button>
          )}
          {caja && (
            <>
              <button className="btn-ghost" onClick={() => setShowEgreso(true)}><Plus size={15}/> Egreso</button>
              <button className="btn-fuchsia" onClick={() => setShowCierre(true)}><Lock size={15}/> Cerrar caja</button>
            </>
          )}
        </div>
      </div>

      {/* estado */}
      {isLoading
        ? <p style={{ color:'var(--dim)', padding:16 }}>Cargando…</p>
        : caja
          ? (
            <div style={{ display:'grid', gridTemplateColumns:'repeat(auto-fill,minmax(180px,1fr))', gap:12 }}>
              {[
                { label:'Apertura inicial', value: fmt(caja.aperturaInicial), color:'var(--yellow)' },
                { label:'Esperado cierre',  value: fmt(caja.cierreEsperado), color:'var(--success)' },
                { label:'Estado',           value: caja.estado,              color:'var(--success)' },
                { label:'Abierta por',      value: caja.abiertaPor ?? '—',   color:'var(--muted)' },
              ].map(k => (
                <div key={k.label} className="card">
                  <p style={{ fontSize:11, color:'var(--muted)', marginBottom:6 }}>{k.label}</p>
                  <p style={{ fontSize:18, fontWeight:800, color:k.color }}>{k.value}</p>
                </div>
              ))}
            </div>
          )
          : (
            <div className="card" style={{ textAlign:'center', padding:40 }}>
              <Lock size={36} color="var(--dim)" style={{ margin:'0 auto 12px' }}/>
              <p style={{ fontWeight:700, color:'var(--muted)', marginBottom:4 }}>No hay caja abierta</p>
              <p style={{ fontSize:13, color:'var(--dim)', marginBottom:16 }}>Abre una caja para registrar movimientos</p>
              <button className="btn-yellow" onClick={() => setShowApertura(true)} style={{ margin:'0 auto' }}>Abrir caja</button>
            </div>
          )
      }

      {/* movimientos */}
      {caja && (
        <div className="card">
          <h2 style={{ fontWeight:700, fontSize:15, marginBottom:14 }}>Movimientos del turno</h2>
          {movimientos.length === 0
            ? <p style={{ fontSize:13, color:'var(--dim)' }}>Sin movimientos</p>
            : movimientos.map(m => (
                <div key={m.id} style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'10px 0', borderBottom:'1px solid var(--border)' }}>
                  <div style={{ display:'flex', alignItems:'center', gap:10 }}>
                    <div style={{ width:32, height:32, borderRadius:'50%', display:'flex', alignItems:'center', justifyContent:'center', background: m.tipo==='INGRESO' ? 'rgba(34,197,94,.15)' : 'rgba(239,68,68,.15)' }}>
                      {m.tipo === 'INGRESO' ? <TrendingUp size={14} color="var(--success)"/> : <TrendingDown size={14} color="var(--error)"/>}
                    </div>
                    <div>
                      <p style={{ fontSize:13, fontWeight:600 }}>{m.concepto}</p>
                      <p style={{ fontSize:11, color:'var(--muted)' }}>{m.metodo ?? '—'} · {format(new Date(m.fecha),'HH:mm',{locale:es})}</p>
                    </div>
                  </div>
                  <span style={{ fontWeight:700, fontSize:13, color: m.tipo==='INGRESO' ? 'var(--success)' : 'var(--error)' }}>
                    {m.tipo==='INGRESO' ? '+' : '-'}{fmt(m.monto)}
                  </span>
                </div>
              ))
          }
        </div>
      )}

      {/* historial */}
      <div className="card" style={{ padding:0, overflow:'hidden' }}>
        <div style={{ padding:'16px 20px', borderBottom:'1px solid var(--border)' }}>
          <h2 style={{ fontWeight:700, fontSize:15 }}>Historial de cajas</h2>
        </div>
        {historial.length === 0
          ? <p style={{ padding:24, fontSize:13, color:'var(--dim)' }}>Sin historial</p>
          : (
            <table className="tbl">
              <thead>
                <tr>{['#','Estado','Apertura','Esperado','Real','Diferencia','Por','Fecha'].map(h => <th key={h}>{h}</th>)}</tr>
              </thead>
              <tbody>
                {historial.map(c => (
                  <tr key={c.id}>
                    <td style={{ color:'var(--muted)' }}>#{c.id}</td>
                    <td><span className={`badge ${c.estado==='ABIERTA'?'badge-abierta':'badge-cerrada'}`}>{c.estado}</span></td>
                    <td style={{ color:'var(--yellow)', fontWeight:700 }}>{fmt(c.aperturaInicial)}</td>
                    <td style={{ color:'var(--muted)' }}>{fmt(c.cierreEsperado)}</td>
                    <td style={{ color:'var(--muted)' }}>{fmt(c.cierreReal)}</td>
                    <td style={{ fontWeight:700, color:(c.diferencia??0)>=0?'var(--success)':'var(--error)' }}>{c.diferencia!=null?fmt(c.diferencia):'—'}</td>
                    <td style={{ color:'var(--muted)', fontSize:12 }}>{c.abiertaPor??'—'}</td>
                    <td style={{ color:'var(--muted)', fontSize:12 }}>{c.abiertaAt?format(new Date(c.abiertaAt),'dd/MM/yy HH:mm',{locale:es}):'—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )
        }
      </div>

      {showApertura && <AperturaModal onClose={() => setShowApertura(false)} />}
      {showCierre && caja && <CierreModal caja={caja} onClose={() => setShowCierre(false)} />}
      {showEgreso && caja && <EgresoModal cajaId={caja.id} onClose={() => setShowEgreso(false)} />}
    </div>
  )
}

function AperturaModal({ onClose }: { onClose: () => void }) {
  const [monto, setMonto] = useState('')
  const m = useMutation({
    mutationFn: () => api.post('/cajas/apertura', { montoInicial: parseFloat(monto) }),
    onSuccess: () => { toast.success('Caja abierta'); queryClient.invalidateQueries({ queryKey: ['caja'] }); queryClient.invalidateQueries({ queryKey: ['cajas'] }); onClose() },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })
  return (
    <SimpleModal title="Abrir caja" onClose={onClose}>
      <Field label="MONTO INICIAL"><input type="number" step="0.01" value={monto} onChange={e => setMonto(e.target.value)} className="input" placeholder="0.00"/></Field>
      <ModalFooter onClose={onClose} onSave={() => m.mutate()} loading={m.isPending} disabled={!monto}/>
    </SimpleModal>
  )
}

function CierreModal({ caja, onClose }: { caja: CajaResponse; onClose: () => void }) {
  const [montoReal, setMontoReal] = useState('')
  const m = useMutation({
    mutationFn: () => api.post(`/cajas/${caja.id}/cierre`, { montoReal: parseFloat(montoReal) }),
    onSuccess: () => { toast.success('Caja cerrada'); queryClient.invalidateQueries({ queryKey: ['caja'] }); queryClient.invalidateQueries({ queryKey: ['cajas'] }); onClose() },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })
  return (
    <SimpleModal title="Cerrar caja" onClose={onClose}>
      <p style={{ fontSize:13, color:'var(--muted)', marginBottom:14 }}>Esperado: <strong style={{ color:'var(--yellow)' }}>{fmt(caja.cierreEsperado)}</strong></p>
      <Field label="MONTO REAL EN CAJA"><input type="number" step="0.01" value={montoReal} onChange={e => setMontoReal(e.target.value)} className="input" placeholder="0.00"/></Field>
      <ModalFooter onClose={onClose} onSave={() => m.mutate()} loading={m.isPending} disabled={!montoReal} label="Cerrar caja"/>
    </SimpleModal>
  )
}

function EgresoModal({ cajaId, onClose }: { cajaId: number; onClose: () => void }) {
  const [concepto, setConcepto] = useState('')
  const [monto,    setMonto]    = useState('')
  const [metodo,   setMetodo]   = useState('EFECTIVO')
  const m = useMutation({
    mutationFn: () => api.post(`/cajas/${cajaId}/egresos`, { concepto, monto: parseFloat(monto), metodo }),
    onSuccess: () => { toast.success('Egreso registrado'); queryClient.invalidateQueries({ queryKey: ['caja', 'movimientos', cajaId] }); onClose() },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })
  return (
    <SimpleModal title="Registrar egreso" onClose={onClose}>
      <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
        <Field label="CONCEPTO"><input value={concepto} onChange={e => setConcepto(e.target.value)} className="input" placeholder="Ej: Compra insumos"/></Field>
        <Field label="MONTO"><input type="number" step="0.01" value={monto} onChange={e => setMonto(e.target.value)} className="input" placeholder="0.00"/></Field>
        <Field label="MÉTODO">
          <select value={metodo} onChange={e => setMetodo(e.target.value)} className="input">
            {['EFECTIVO','TRANSFERENCIA','TARJETA'].map(x => <option key={x}>{x}</option>)}
          </select>
        </Field>
      </div>
      <ModalFooter onClose={onClose} onSave={() => m.mutate()} loading={m.isPending} disabled={!concepto || !monto}/>
    </SimpleModal>
  )
}

function SimpleModal({ title, children, onClose }: { title: string; children: React.ReactNode; onClose: () => void }) {
  return (
    <div className="overlay">
      <div style={{ width:'100%', maxWidth:380, background:'var(--surface)', border:'1px solid var(--border)', borderRadius:20, padding:24 }}>
        <div style={{ display:'flex', justifyContent:'space-between', marginBottom:18 }}>
          <h2 style={{ fontWeight:700, fontSize:16 }}>{title}</h2>
          <button onClick={onClose} style={{ background:'none', border:'none', cursor:'pointer', color:'var(--muted)' }}><X size={18}/></button>
        </div>
        {children}
      </div>
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:6 }}>{label}</label>
      {children}
    </div>
  )
}

function ModalFooter({ onClose, onSave, loading, disabled, label = 'Guardar' }: { onClose: () => void; onSave: () => void; loading: boolean; disabled: boolean; label?: string }) {
  return (
    <div style={{ display:'flex', gap:8, marginTop:20 }}>
      <button className="btn-yellow" style={{ flex:1, justifyContent:'center' }} onClick={onSave} disabled={loading || disabled}>
        <Check size={14}/> {loading ? 'Guardando…' : label}
      </button>
      <button className="btn-ghost" onClick={onClose}>Cancelar</button>
    </div>
  )
}
