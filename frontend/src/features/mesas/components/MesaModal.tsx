'use client'
import { useState } from 'react'
import { X, Check } from 'lucide-react'
import { useGuardarMesa } from '../hooks'
import type { Mesa, MesaPayload } from '../types'

export default function MesaModal({ editing, onClose }: { editing: Mesa | null; onClose: () => void }) {
  const [numero,    setNumero]    = useState(editing?.numero?.toString() ?? '')
  const [capacidad, setCapacidad] = useState(editing?.capacidad?.toString() ?? '4')
  const [ubicacion, setUbicacion] = useState(editing?.ubicacion ?? '')

  const mutation = useGuardarMesa(onClose)

  const guardar = () => {
    const body: MesaPayload = { numero: parseInt(numero), capacidad: parseInt(capacidad), ubicacion: ubicacion || null }
    mutation.mutate({ id: editing?.id, body })
  }

  return (
    <div className="overlay">
      <div style={{ width:'100%', maxWidth:380, background:'var(--surface)', border:'1px solid var(--border)', borderRadius:20, padding:24 }}>
        <div style={{ display:'flex', justifyContent:'space-between', marginBottom:20 }}>
          <h2 style={{ fontWeight:700, fontSize:16 }}>{editing ? 'Editar mesa' : 'Nueva mesa'}</h2>
          <button onClick={onClose} style={{ background:'none', border:'none', cursor:'pointer', color:'var(--muted)' }}><X size={18}/></button>
        </div>
        <div style={{ display:'flex', flexDirection:'column', gap:14 }}>
          {[
            { label:'NÚMERO',          value:numero,    set:setNumero,    type:'number' },
            { label:'CAPACIDAD',       value:capacidad, set:setCapacidad, type:'number' },
            { label:'UBICACIÓN (opcional)', value:ubicacion, set:setUbicacion, type:'text' },
          ].map(({ label, value, set, type }) => (
            <div key={label}>
              <label style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:6 }}>{label}</label>
              <input type={type} value={value} onChange={e => set(e.target.value)} className="input"/>
            </div>
          ))}
        </div>
        <div style={{ display:'flex', gap:8, marginTop:20 }}>
          <button className="btn-yellow" style={{ flex:1, justifyContent:'center' }} onClick={guardar} disabled={mutation.isPending || !numero}>
            <Check size={14}/> {mutation.isPending ? 'Guardando…' : 'Guardar'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}
