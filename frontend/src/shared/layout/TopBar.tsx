'use client'
import { ChevronDown, LogOut, Settings, UserRound } from 'lucide-react'
import { usePathname, useRouter } from 'next/navigation'
import { useAuthStore } from '@/features/auth'
import { getTurno } from '@/shared/lib/utils'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'
import toast from 'react-hot-toast'

export default function TopBar() {
  const { user, logout } = useAuthStore()
  const router = useRouter()
  const pathname = usePathname()
  const section = ({ dashboard: 'Resumen', pedidos: 'Pedidos', cocina: 'Cocina', mesas: 'Mesas', cuentas: 'Cuentas', caja: 'Caja', entregas: 'Entregas', productos: 'Productos', usuarios: 'Usuarios', reportes: 'Reportes', anulaciones: 'Anulaciones', auditoria: 'Auditoría' } as Record<string, string>)[pathname.split('/')[1]] || 'Panel'
  const now = new Date()
  const name = user?.nombre || user?.username || 'Equipo'

  async function handleLogout() {
    await logout()
    toast.success('Sesión cerrada')
    router.push('/login')
  }

  return (
    <header className="topbar">
      <div className="topbar-greeting">
        <div className="topbar-sign" aria-hidden="true"><span>EB</span></div>
        <div className="topbar-title"><span>Estación Burger</span><p>{section}</p></div>
      </div>
      <div className="topbar-date"><strong>{getTurno()}</strong><span>{format(now, "EEEE, d 'de' MMMM", { locale: es })}</span></div>
      <details className="account-menu" onKeyDown={(event) => {
        if (event.key === 'Escape') {
          event.currentTarget.open = false
          event.currentTarget.querySelector('summary')?.focus()
        }
      }}>
        <summary className="account-trigger" aria-label="Menú de cuenta">
          <span className="account-avatar" aria-hidden="true">{name.charAt(0).toUpperCase()}</span>
          <span className="account-trigger-text"><strong>{name}</strong><small>{user?.rol || 'Usuario'}</small></span>
          <ChevronDown size={16} aria-hidden="true" />
        </summary>
        <div className="account-panel">
          <div className="account-panel-heading"><strong>{name}</strong><span>{user?.username}</span></div>
          <details className="account-section">
            <summary><UserRound size={17} aria-hidden="true" /> Mi perfil</summary>
            <div className="account-section-content"><span>Nombre: {name}</span><span>Usuario: {user?.username}</span><span>Rol: {user?.rol}</span></div>
          </details>
          <details className="account-section">
            <summary><Settings size={17} aria-hidden="true" /> Configuración</summary>
            <div className="account-section-content">Las opciones de configuración estarán disponibles próximamente.</div>
          </details>
          <button className="account-logout" onClick={handleLogout}><LogOut size={17} aria-hidden="true" /> Cerrar sesión</button>
        </div>
      </details>
    </header>
  )
}
