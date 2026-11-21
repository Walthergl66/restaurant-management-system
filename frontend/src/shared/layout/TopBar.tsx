'use client'
import { ChevronDown, LogOut, Menu, PanelLeftClose, PanelLeftOpen, Settings, UserRound, X } from 'lucide-react'
import Image from 'next/image'
import { usePathname, useRouter } from 'next/navigation'
import { useAuthStore } from '@/features/auth'
import toast from 'react-hot-toast'

const HEADER_SUBTITLES: Record<string, string> = {
  '/dashboard': 'Resumen operativo',
  '/pedidos': 'Pedidos',
  '/comandas': 'Comandas',
  '/mesas': 'Mesas',
  '/cuentas': 'Cuentas',
  '/caja': 'Caja del turno',
  '/entregas': 'Entregas',
  '/productos': 'Catálogo',
  '/usuarios': 'Usuarios y roles',
  '/reportes': 'Reportes',
  '/anulaciones': 'Anulaciones',
  '/auditoria': 'Auditoría',
}

type TopBarProps = {
  sidebarCollapsed: boolean
  mobileMenuOpen: boolean
  onToggleSidebar: () => void
  onToggleMobileMenu: () => void
}

export default function TopBar({ sidebarCollapsed, mobileMenuOpen, onToggleSidebar, onToggleMobileMenu }: TopBarProps) {
  const { user, logout } = useAuthStore()
  const router = useRouter()
  const pathname = usePathname()
  const headerSubtitle = pathname.startsWith('/pedidos/')
    ? 'Detalle del pedido'
    : HEADER_SUBTITLES[pathname] ?? 'Administración'
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
      <button
        className="sidebar-toggle sidebar-toggle-desktop"
        type="button"
        aria-label={sidebarCollapsed ? 'Expandir barra lateral' : 'Contraer barra lateral'}
        aria-controls="primary-sidebar"
        aria-expanded={!sidebarCollapsed}
        onClick={onToggleSidebar}
      >
        {sidebarCollapsed ? <PanelLeftOpen size={19} aria-hidden="true" /> : <PanelLeftClose size={19} aria-hidden="true" />}
      </button>
      <button
        className="sidebar-toggle sidebar-toggle-mobile"
        type="button"
        aria-label={mobileMenuOpen ? 'Cerrar navegación' : 'Abrir navegación'}
        aria-controls="primary-sidebar"
        aria-expanded={mobileMenuOpen}
        onClick={onToggleMobileMenu}
      >
        {mobileMenuOpen ? <X size={20} aria-hidden="true" /> : <Menu size={20} aria-hidden="true" />}
      </button>
      <div className="topbar-greeting">
        <div className="topbar-dashboard-title"><h1>Kitchen pass</h1><span>{headerSubtitle}</span></div>
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
