'use client'
import { useState } from 'react'
import { Check, X } from 'lucide-react'
import { fmt, METODOS_PAGO } from '@/shared/lib/utils'
import { useCobrar } from '../hooks'
import type { CuentaResponse } from '../types'

export default function CobrarModal({ cuenta, onClose }: { cuenta: CuentaResponse; onClose: () => void }) {
  const [metodo, setMetodo] = useState('EFECTIVO')
  const [monto,  setMonto]  = useState(cuenta.total?.toString() ?? '')

  const m = useCobrar({ onCobrado: onClose })

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
          <button className="btn-yellow" style={{ flex:1, justifyContent:'center' }} onClick={() => m.mutate({ id: cuenta.id, payload: { pagos: [{ metodo, monto: parseFloat(monto) }], documento: null } })} disabled={m.isPending || !monto}>
            <Check size={14}/> {m.isPending ? 'Procesando…' : 'Confirmar pago'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}
