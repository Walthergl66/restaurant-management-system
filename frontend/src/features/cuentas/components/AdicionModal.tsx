'use client'
import { useState } from 'react'
import { Check, X } from 'lucide-react'
import { useCrearAdicion } from '../hooks'
import { nuevoCodigoAdicion } from '../lib/codigo'
import type { CuentaResponse } from '../types'

export default function AdicionModal({ cuenta, onClose }: { cuenta: CuentaResponse; onClose: () => void }) {
  const [notas, setNotas] = useState('')

  const m = useCrearAdicion({ onCreated: onClose })

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
          <button className="btn-yellow" style={{ flex:1, justifyContent:'center' }} onClick={() => m.mutate({ id: cuenta.id, payload: { codigo: nuevoCodigoAdicion(), notas: notas || null } })} disabled={m.isPending}>
            <Check size={14}/> {m.isPending ? 'Creando…' : 'Crear adición'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}
