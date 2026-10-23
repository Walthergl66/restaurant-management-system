'use client'
import { useAuthStore, hasPermiso } from '@/features/auth'
import RepartidoresPanel from './RepartidoresPanel'
import PendientesPanel from './PendientesPanel'
import EstadosRepartoPanel from './EstadosRepartoPanel'

export default function EntregasScreen() {
  const user = useAuthStore(s => s.user)
  const puedeVer = hasPermiso(user, 'delivery:asignaciones-gestionar')

  if (!puedeVer) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <h1 style={{ fontSize: 20, fontWeight: 800 }}>Gestión de entregas</h1>
        <div className="card" style={{ padding: 32, textAlign: 'center', color: 'var(--muted)' }}>
          No tienes permiso para gestionar entregas.
        </div>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <h1 style={{ fontSize: 20, fontWeight: 800 }}>Gestión de entregas</h1>
      <RepartidoresPanel />
      <PendientesPanel />
      <EstadosRepartoPanel />
    </div>
  )
}
