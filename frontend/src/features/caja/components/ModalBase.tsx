'use client'
import type { ReactNode } from 'react'
import { X, Check } from 'lucide-react'

export function SimpleModal({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
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

export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <label style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:6 }}>{label}</label>
      {children}
    </div>
  )
}

export function ModalFooter({ onClose, onSave, loading, disabled, label = 'Guardar' }: { onClose: () => void; onSave: () => void; loading: boolean; disabled: boolean; label?: string }) {
  return (
    <div style={{ display:'flex', gap:8, marginTop:20 }}>
      <button className="btn-yellow" style={{ flex:1, justifyContent:'center' }} onClick={onSave} disabled={loading || disabled}>
        <Check size={14}/> {loading ? 'Guardando…' : label}
      </button>
      <button className="btn-ghost" onClick={onClose}>Cancelar</button>
    </div>
  )
}
