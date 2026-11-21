'use client'
import { useState } from 'react'
import { X, Check } from 'lucide-react'
import { useGuardarMesa, useZonas } from '../hooks'
import type { Mesa, MesaPayload } from '../types'
import ZonaModal from './ZonaModal'

const NUEVA_ZONA = '__nueva__'

export default function MesaModal({ editing, onClose }: { editing: Mesa | null; onClose: () => void }) {
  const [numero,    setNumero]    = useState(editing?.numero?.toString() ?? '')
  const [capacidad, setCapacidad] = useState(editing?.capacidad?.toString() ?? '4')
  const [zonaElegida, setZonaElegida] = useState<string | null>(editing ? String(editing.zonaId) : null)
  const [creandoZona, setCreandoZona] = useState(false)

  const zonasQuery = useZonas()
  const zonas = zonasQuery.data ?? []
  // Sin elección explícita, la mesa nueva va a la primera zona (el orden lo define el administrador).
  const zonaId = zonaElegida ?? (zonas[0] ? String(zonas[0].id) : '')

  const mutation = useGuardarMesa(onClose)

  const guardar = () => {
    const body: MesaPayload = {
      numero: parseInt(numero),
      capacidad: parseInt(capacidad),
      ...(zonaId ? { zonaId: Number(zonaId) } : {}),
    }
    mutation.mutate({ id: editing?.id, body })
  }

  const cambiarZona = (valor: string) => {
    if (valor === NUEVA_ZONA) setCreandoZona(true)
    else setZonaElegida(valor)
  }

  const campos = [
    { label: 'NÚMERO',    value: numero,    set: setNumero },
    { label: 'CAPACIDAD', value: capacidad, set: setCapacidad },
  ]

  return (
    <div className="overlay">
      <div style={{ width:'100%', maxWidth:380, background:'var(--surface)', border:'1px solid var(--border)', borderRadius:20, padding:24 }}>
        <div style={{ display:'flex', justifyContent:'space-between', marginBottom:20 }}>
          <h2 style={{ fontWeight:700, fontSize:16 }}>{editing ? 'Editar mesa' : 'Nueva mesa'}</h2>
          <button onClick={onClose} style={{ background:'none', border:'none', cursor:'pointer', color:'var(--muted)' }}><X size={18}/></button>
        </div>
        <div style={{ display:'flex', flexDirection:'column', gap:14 }}>
          {campos.map(({ label, value, set }) => (
            <div key={label}>
              <label style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:6 }}>{label}</label>
              <input type="number" value={value} onChange={e => set(e.target.value)} className="input"/>
            </div>
          ))}
          <div>
            <label htmlFor="mesa-zona" style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:6 }}>ZONA</label>
            <select id="mesa-zona" className="input" value={zonaId} onChange={e => cambiarZona(e.target.value)} disabled={zonasQuery.isLoading}>
              {zonas.map(zona => <option key={zona.id} value={zona.id}>{zona.nombre}</option>)}
              <option value={NUEVA_ZONA}>+ Nueva zona…</option>
            </select>
            {zonasQuery.isError && <small className="field-error">No se pudieron cargar las zonas.</small>}
          </div>
        </div>
        <div style={{ display:'flex', gap:8, marginTop:20 }}>
          <button className="btn-yellow" style={{ flex:1, justifyContent:'center' }} onClick={guardar} disabled={mutation.isPending || !numero || !zonaId}>
            <Check size={14}/> {mutation.isPending ? 'Guardando…' : 'Guardar'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>

      {creandoZona && (
        <ZonaModal
          editing={null}
          onClose={() => setCreandoZona(false)}
          onSaved={zona => setZonaElegida(String(zona.id))}
        />
      )}
    </div>
  )
}
