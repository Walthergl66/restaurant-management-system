'use client'
import { useAuthStore, hasPermiso } from '@/features/auth'
import VentasDashboard from './VentasDashboard'
import SalonDashboard from './SalonDashboard'
import CocinaDashboard from './CocinaDashboard'

/*
 * El dashboard se resuelve por PERMISO, no por nombre de rol:
 *  - reportes:ver  → panel de Ventas/Caja (ADMIN, que ve todo y cobra)
 *  - pedidos:crear → panel de Salón (MESERO)
 *  - resto         → panel de Cocina (COCINA)
 */
export default function DashboardScreen() {
  const { user } = useAuthStore()

  if (!user) return null
  if (hasPermiso(user, 'reportes:ver')) return <VentasDashboard />
  if (hasPermiso(user, 'pedidos:crear')) return <SalonDashboard />
  return <CocinaDashboard />
}
