'use client'
import { useState } from 'react'
import { Check, Hash, Users, X } from 'lucide-react'
import { mesaCode, siguienteNumeroMesa } from '@/shared/lib/mesaCode'
import { useGuardarMesa, useZonas } from '../hooks'
import type { Mesa, MesaPayload } from '../types'
import ZonaModal from './ZonaModal'

const NUEVA_ZONA = '__nueva__'

export default function MesaModal({ editing, mesas, onClose }: { editing: Mesa | null; mesas: Mesa[]; onClose: () => void }) {
  const sugerido = siguienteNumeroMesa(mesas.map(mesa => mesa.numero))
  const [numero, setNumero] = useState(editing?.numero?.toString() ?? sugerido?.toString() ?? '')
  const [capacidad, setCapacidad] = useState(editing?.capacidad?.toString() ?? '4')
  const [zonaElegida, setZonaElegida] = useState<string | null>(editing ? String(editing.zonaId) : null)
  const [creandoZona, setCreandoZona] = useState(false)
  const zonasQuery = useZonas()
  const zonas = zonasQuery.data ?? []
  const zonaId = zonaElegida ?? (zonas[0] ? String(zonas[0].id) : '')
  const mutation = useGuardarMesa(onClose)
  const numeroValor = Number(numero)
  const capacidadValor = Number(capacidad)
  const duplicada = mesas.find(mesa => mesa.numero === numeroValor && mesa.id !== editing?.id)
  // Las mesas heredadas fuera del rango nuevo siguen siendo editables sin renumerarlas.
  const numeroValido = /^\d+$/.test(numero) && (numeroValor >= 1 && numeroValor <= 99 || numeroValor === editing?.numero)
  const capacidadValida = /^\d+$/.test(capacidad) && capacidadValor >= 1 && capacidadValor <= 50
  const puedeGuardar = numeroValido && capacidadValida && !duplicada && !!zonaId && !mutation.isPending

  const guardar = () => {
    if (!puedeGuardar) return
    const body: MesaPayload = { numero: numeroValor, capacidad: capacidadValor, zonaId: Number(zonaId) }
    mutation.mutate({ id: editing?.id, body })
  }

  const cambiarZona = (valor: string) => {
    if (valor === NUEVA_ZONA) setCreandoZona(true)
    else setZonaElegida(valor)
  }

  return (
    <div className="overlay" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) onClose() }}>
      <div role="dialog" aria-modal="true" aria-labelledby="mesa-modal-title" style={{ width:'100%', maxWidth:440, maxHeight:'90vh', overflowY:'auto', background:'var(--surface)', border:'1px solid var(--border)', borderRadius:20, padding:24, boxShadow:'0 20px 60px rgba(0,0,0,.2)' }}>
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', gap:12 }}>
          <div>
            <p style={{ fontSize:11, fontWeight:800, letterSpacing:1.2, textTransform:'uppercase', color:'var(--muted)' }}>Catálogo de mesas</p>
            <h2 id="mesa-modal-title" style={{ fontWeight:800, fontSize:21, marginTop:4 }}>{editing ? 'Editar mesa' : 'Nueva mesa'}</h2>
            <p style={{ color:'var(--muted)', fontSize:12, marginTop:4 }}>Asigna un código visible, la capacidad y la zona.</p>
          </div>
          <button type="button" onClick={onClose} aria-label="Cerrar" style={{ background:'none', border:'none', cursor:'pointer', color:'var(--muted)', padding:4 }}><X size={18}/></button>
        </div>

        <div style={{ background:'var(--surface-2)', border:'1px solid var(--border)', borderRadius:14, padding:'14px 16px', marginTop:22, display:'flex', alignItems:'center', justifyContent:'space-between', gap:12 }}>
          <div><p style={{ color:'var(--muted)', fontSize:11, fontWeight:700, textTransform:'uppercase', letterSpacing:.8 }}>Vista previa</p><p style={{ fontSize:12, color:'var(--muted)', marginTop:3 }}>Así se identificará en el salón</p></div>
          <strong style={{ fontSize:26, letterSpacing:1, color:'var(--text)' }}>{numeroValido ? mesaCode(numeroValor) : 'M—'}</strong>
        </div>

        <div style={{ display:'grid', gap:16, marginTop:20 }}>
          <div>
            <label htmlFor="mesa-numero" style={{ fontSize:12, fontWeight:800, display:'block', marginBottom:6 }}>Número de mesa <span style={{ color:'var(--muted)', fontWeight:500 }}>(01–99)</span></label>
            <div style={{ position:'relative' }}><Hash size={16} style={{ position:'absolute', left:12, top:14, color:'var(--muted)' }}/><input id="mesa-numero" type="number" min="1" max="99" step="1" inputMode="numeric" value={numero} onChange={event => setNumero(event.target.value)} className="input" style={{ paddingLeft:38 }} aria-invalid={!!numero && (!numeroValido || !!duplicada)} aria-describedby="mesa-numero-ayuda" /></div>
            <p id="mesa-numero-ayuda" style={{ fontSize:12, marginTop:6, color: duplicada || (numero && !numeroValido) ? 'var(--error)' : 'var(--muted)' }}>
              {duplicada ? `${mesaCode(numeroValor)} ya existe${duplicada.activo ? '' : ' y está deshabilitada'}. ${duplicada.activo ? 'Elige otro número.' : 'Habilítala en la lista en lugar de crear otra.'}` : numero && !numeroValido ? 'Ingresa un número entero entre 1 y 99.' : editing && editing.numero > 99 && numeroValor === editing.numero ? 'Código anterior conservado. Para cambiarlo, elige un número de 1 a 99.' : sugerido && !editing ? `Siguiente disponible: ${mesaCode(sugerido)}. Puedes elegir otro código libre.` : 'Cada número identifica una sola mesa en todo el restaurante.'}
            </p>
          </div>
          <div>
            <label htmlFor="mesa-capacidad" style={{ fontSize:12, fontWeight:800, display:'block', marginBottom:6 }}>Capacidad</label>
            <div style={{ position:'relative' }}><Users size={16} style={{ position:'absolute', left:12, top:14, color:'var(--muted)' }}/><input id="mesa-capacidad" type="number" min="1" max="50" step="1" inputMode="numeric" value={capacidad} onChange={event => setCapacidad(event.target.value)} className="input" style={{ paddingLeft:38 }} aria-invalid={!!capacidad && !capacidadValida} aria-describedby="mesa-capacidad-ayuda" /></div>
            <p id="mesa-capacidad-ayuda" style={{ fontSize:12, marginTop:6, color: capacidad && !capacidadValida ? 'var(--error)' : 'var(--muted)' }}>{capacidad && !capacidadValida ? 'Indica entre 1 y 50 personas.' : 'Número de personas que pueden sentarse.'}</p>
          </div>
          <div>
            <label htmlFor="mesa-zona" style={{ fontSize:12, fontWeight:800, display:'block', marginBottom:6 }}>Zona</label>
            <select id="mesa-zona" className="input" value={zonaId} onChange={event => cambiarZona(event.target.value)} disabled={zonasQuery.isLoading}>
              {zonas.map(zona => <option key={zona.id} value={zona.id}>{zona.nombre}</option>)}
              <option value={NUEVA_ZONA}>+ Nueva zona…</option>
            </select>
            {zonasQuery.isError ? <p style={{ color:'var(--error)', fontSize:12, marginTop:6 }}>No se pudieron cargar las zonas.</p> : <p style={{ color:'var(--muted)', fontSize:12, marginTop:6 }}>Ubicación de la mesa dentro del restaurante.</p>}
          </div>
        </div>
        {!sugerido && !editing && <p role="alert" style={{ color:'var(--error)', fontSize:12, marginTop:16 }}>Ya están ocupados los códigos M01 a M99.</p>}
        <div style={{ display:'flex', gap:8, marginTop:22 }}>
          <button className="btn-yellow" style={{ flex:1, justifyContent:'center' }} onClick={guardar} disabled={!puedeGuardar}><Check size={14}/> {mutation.isPending ? 'Guardando…' : editing ? 'Guardar cambios' : 'Crear mesa'}</button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
      {creandoZona && <ZonaModal editing={null} onClose={() => setCreandoZona(false)} onSaved={zona => { setZonaElegida(String(zona.id)); setCreandoZona(false) }} />}
    </div>
  )
}
