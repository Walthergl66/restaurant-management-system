'use client'
import { useState } from 'react'
import { useEgreso } from '../hooks'
import { SimpleModal, Field, ModalFooter } from './ModalBase'

export default function EgresoModal({ cajaId, onClose }: { cajaId: number; onClose: () => void }) {
  const [concepto, setConcepto] = useState('')
  const [monto,    setMonto]    = useState('')
  const [metodo,   setMetodo]   = useState('EFECTIVO')
  const m = useEgreso({ onRegistrado: onClose })

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
      <ModalFooter onClose={onClose} onSave={() => m.mutate({ cajaId, payload: { concepto, monto: parseFloat(monto), metodo } })} loading={m.isPending} disabled={!concepto || !monto}/>
    </SimpleModal>
  )
}
