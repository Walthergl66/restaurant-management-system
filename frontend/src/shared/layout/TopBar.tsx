'use client'
import { ChevronDown, LogOut, Settings, UserRound } from 'lucide-react'
import Image from 'next/image'
import { usePathname, useRouter } from 'next/navigation'
import { hasPermiso, useAuthStore } from '@/features/auth'
import toast from 'react-hot-toast'

export default function TopBar() {
  const { user, logout } = useAuthStore()
  const router = useRouter()
  const pathname = usePathname()
  const showDashboardTitle = pathname === '/dashboard' && hasPermiso(user, 'reportes:ver')
  const name = user?.nombre || user?.username || 'Equipo'

  async function handleLogout() {
    await logout()
    toast.success('Sesión cerrada')
    router.push('/login')
  }

  return (
    <header className="topbar">
      <div className="topbar-brand" aria-label="Estación Burger">
        <Image src="/login-logo-propuesta.png" alt="Estación Burger" width={270} height={220} priority />
      </div>
      <div className="topbar-greeting">
        {showDashboardTitle && <div className="topbar-dashboard-title"><h1>Kitchen pass</h1><span>Resumen operativo</span></div>}
      </div>
      <details className="account-menu" onKeyDown={(event) => {
        if (event.key === 'Escape') {
          event.currentTarget.open = false
          event.currentTarget.querySelector('summary')?.focus()
        }
      }}>
        <summary className="account-trigger" aria-label="Menú de cuenta">
          <span className="account-avatar" aria-hidden="true"><UserRound size={21} /></span>
          <span className="account-trigger-text"><strong>{name}</strong><small>{user?.rol || 'Equipo'}</small></span>
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
