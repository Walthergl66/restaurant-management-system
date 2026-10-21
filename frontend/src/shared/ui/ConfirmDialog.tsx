'use client'
import { AlertTriangle } from 'lucide-react'

type ConfirmDialogProps = {
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  variant?: 'danger' | 'primary'
  pending?: boolean
  onConfirm: () => void
  onClose: () => void
}

export default function ConfirmDialog({
  title,
  message,
  confirmText = 'Aceptar',
  cancelText = 'Cancelar',
  variant = 'danger',
  pending = false,
  onConfirm,
  onClose,
}: ConfirmDialogProps) {
  return (
    <div className="overlay" role="alertdialog" aria-modal="true" aria-labelledby="confirm-dialog-title" aria-describedby="confirm-dialog-message">
      <div className="admin-modal confirm-dialog">
        <AlertTriangle size={34} className="warning-icon" />
        <h2 id="confirm-dialog-title">{title}</h2>
        <p id="confirm-dialog-message">{message}</p>
        <div className="modal-actions">
          <button className={variant === 'primary' ? 'btn-yellow' : 'btn-danger'} onClick={onConfirm} disabled={pending}>
            {pending ? 'Guardando…' : confirmText}
          </button>
          <button className="btn-ghost" onClick={onClose} disabled={pending}>{cancelText}</button>
        </div>
      </div>
    </div>
  )
}
