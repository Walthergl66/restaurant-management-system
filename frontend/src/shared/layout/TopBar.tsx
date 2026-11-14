'use client'
import { ChevronDown, LogOut, Settings, UserRound } from 'lucide-react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/features/auth'
import toast from 'react-hot-toast'

export default function TopBar() {
  const { user, logout } = useAuthStore()
  const router = useRouter()
  const name = user?.nombre || user?.username || 'Equipo'

  async function handleLogout() {
    await logout()
    toast.success('Sesión cerrada')
    router.push('/login')
  }

  return (
    <header className="topbar">
      <div className="topbar-greeting">
      </div>
      <details className="account-menu" onKeyDown={(event) => {
        if (event.key === 'Escape') {
          event.currentTarget.open = false
          event.currentTarget.querySelector('summary')?.focus()
        }
      }}>
        <summary className="account-trigger" aria-label="Menú de cuenta">
          <span className="account-avatar" aria-hidden="true"><UserRound size={21} /></span>
          <span className="account-trigger-text"><strong>{name}</strong></span>
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
