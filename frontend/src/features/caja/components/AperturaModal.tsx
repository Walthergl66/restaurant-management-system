'use client'
import { useState } from 'react'
import { useAbrirCaja } from '../hooks'
import { SimpleModal, Field, ModalFooter } from './ModalBase'

export default function AperturaModal({ onClose }: { onClose: () => void }) {
  const [monto, setMonto] = useState('')
  const m = useAbrirCaja({ onAbierto: onClose })

  return (
    <SimpleModal title="Abrir caja" onClose={onClose}>
      <Field label="MONTO INICIAL"><input type="number" step="0.01" value={monto} onChange={e => setMonto(e.target.value)} className="input" placeholder="0.00"/></Field>
      <ModalFooter onClose={onClose} onSave={() => m.mutate(parseFloat(monto))} loading={m.isPending} disabled={!monto}/>
    </SimpleModal>
  )
}
