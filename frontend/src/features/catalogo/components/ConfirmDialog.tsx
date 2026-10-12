'use client'
import { AlertTriangle } from 'lucide-react'

export default function ConfirmDialog({ title, message, pending, activating = false, onConfirm, onClose }: { title: string; message: string; pending?: boolean; activating?: boolean; onConfirm: () => void; onClose: () => void }) {
  return <div className="overlay" role="alertdialog" aria-modal="true" aria-labelledby="catalogo-confirm-title" aria-describedby="catalogo-confirm-message"><div className="admin-modal confirm-dialog"><AlertTriangle size={34} className="warning-icon"/><h2 id="catalogo-confirm-title">{title}</h2><p id="catalogo-confirm-message">{message}</p><div className="modal-actions"><button className={activating ? 'btn-yellow' : 'btn-danger'} onClick={onConfirm} disabled={pending}>{pending ? 'Guardando…' : activating ? 'Activar' : 'Desactivar'}</button><button className="btn-ghost" onClick={onClose} disabled={pending}>Cancelar</button></div></div></div>
}
