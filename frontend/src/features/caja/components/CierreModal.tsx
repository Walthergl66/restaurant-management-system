'use client'
import { useState } from 'react'
import { fmt } from '@/shared/lib/utils'
import { useCerrarCaja } from '../hooks'
import type { CajaResponse } from '../types'
import { SimpleModal, Field, ModalFooter } from './ModalBase'

export default function CierreModal({ caja, onClose }: { caja: CajaResponse; onClose: () => void }) {
  const [montoReal, setMontoReal] = useState('')
  const m = useCerrarCaja({ onCerrado: onClose })

  return (
    <SimpleModal title="Cerrar caja" onClose={onClose}>
      <p style={{ fontSize:13, color:'var(--muted)', marginBottom:14 }}>Esperado: <strong style={{ color:'var(--yellow)' }}>{fmt(caja.cierreEsperado)}</strong></p>
      <Field label="MONTO REAL EN CAJA"><input type="number" step="0.01" value={montoReal} onChange={e => setMontoReal(e.target.value)} className="input" placeholder="0.00"/></Field>
      <ModalFooter onClose={onClose} onSave={() => m.mutate({ id: caja.id, montoReal: parseFloat(montoReal) })} loading={m.isPending} disabled={!montoReal} label="Cerrar caja"/>
    </SimpleModal>
  )
}
