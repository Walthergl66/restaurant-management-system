'use client'
import { useAuthStore, hasPermiso } from '@/features/auth'
import VentasDashboard from './VentasDashboard'

/*
 * La web es para la administración y la caja: el resumen es el panel de ventas, que se muestra a
 * quien tenga permiso de reportes. Sin ese permiso no hay panel que mostrar.
 */
export default function DashboardScreen() {
  const { user } = useAuthStore()

  if (!user) return null
  if (hasPermiso(user, 'reportes:ver')) return <VentasDashboard />
  return (
    <div className="card" style={{ padding: 32, textAlign: 'center' }}>
      <p style={{ fontWeight: 700 }}>Sin panel disponible</p>
      <p style={{ color: 'var(--muted)', fontSize: 12, marginTop: 6 }}>Tu usuario no tiene permiso para ver el resumen de ventas.</p>
    </div>
  )
}
