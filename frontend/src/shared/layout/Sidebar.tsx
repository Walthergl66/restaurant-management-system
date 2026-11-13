'use client'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import {
  LayoutDashboard, ShoppingBag, Package, Table2, DollarSign,
  Users, XCircle, ClipboardList, BarChart3, LogOut, ChefHat, Receipt, Truck,
} from 'lucide-react'
import { useAuthStore } from '@/features/auth'
import { hasPermiso } from '@/shared/lib/permissions'
import Logo from '@/shared/ui/Logo'
import toast from 'react-hot-toast'

const NAV: { href: string; label: string; icon: typeof LayoutDashboard; group: 'Operación' | 'Administración' | 'Control'; permisos?: string[] }[] = [
  { href: '/dashboard', group: 'Operación',   label: 'Resumen',    icon: LayoutDashboard },
  { href: '/pedidos', group: 'Operación',     label: 'Pedidos',    icon: ShoppingBag,  permisos: ['pedidos:ver'] },
  { href: '/cocina', group: 'Operación',      label: 'Cocina',     icon: ChefHat,      permisos: ['comandas:ver'] },
  { href: '/mesas', group: 'Operación',       label: 'Mesas',      icon: Table2,       permisos: ['mesas:gestionar'] },
  { href: '/productos', group: 'Administración',   label: 'Productos',  icon: Package,      permisos: ['catalogo:ver'] },
  { href: '/cuentas', group: 'Operación',     label: 'Cuentas',    icon: Receipt,      permisos: ['cuentas:ver'] },
  { href: '/caja', group: 'Operación',        label: 'Caja',       icon: DollarSign,   permisos: ['caja:movimientos'] },
  { href: '/reportes', group: 'Control',    label: 'Reportes',   icon: BarChart3,    permisos: ['reportes:ver'] },
  { href: '/usuarios', group: 'Administración',    label: 'Usuarios',   icon: Users,        permisos: ['usuarios:ver'] },
  { href: '/anulaciones', group: 'Control', label: 'Anulaciones',icon: XCircle,      permisos: ['anulaciones:solicitar', 'anulaciones:aprobar'] },
  { href: '/entregas', group: 'Operación',    label: 'Entregas',   icon: Truck,        permisos: ['delivery:asignaciones-gestionar'] },
  { href: '/auditoria', group: 'Control',   label: 'Auditoría',  icon: ClipboardList,permisos: ['auditoria:ver'] },
]

export default function Sidebar() {
  const pathname = usePathname()
  const router   = useRouter()
  const { user, logout } = useAuthStore()

  const nav = NAV.filter((item) => !item.permisos || hasPermiso(user, ...item.permisos))

  const handleLogout = async () => {
    await logout()
    toast.success('Sesión cerrada')
    router.push('/login')
  }

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <Logo size={104} />
        <div className="sidebar-brand-name"><strong>ESTACIÓN</strong><span>BURGER</span></div>
      </div>
      <nav className="sidebar-nav" aria-label="Navegación principal">
        {(['Operación', 'Administración', 'Control'] as const).map((group) => {
          const items = nav.filter((item) => item.group === group)
          if (!items.length) return null
          return <div className="sidebar-group" key={group}>
            <p className="sidebar-group-label">{group}</p>
            {items.map(({ href, label, icon: Icon }) => {
              const active = pathname === href || (href !== '/dashboard' && pathname.startsWith(`${href}/`))
              return <Link key={href} href={href} className={`nav-item${active ? ' active' : ''}`} aria-current={active ? 'page' : undefined} aria-label={label} title={label}>
                <Icon size={18} strokeWidth={1.8} aria-hidden="true" />
                <span className="sidebar-item-label">{label}</span>
              </Link>
            })}
          </div>
        })}
      </nav>

      {/* Footer */}
      <div style={{ borderTop: '1px solid var(--border)', paddingTop: 12, marginTop: 12 }}>
        {user && (
          <div className="sidebar-user" style={{ padding: '0 4px', marginBottom: 8 }}>
            <p style={{ fontSize: 12, fontWeight: 600, color: 'var(--text)' }}>{user.username}</p>
            <p style={{ fontSize: 11, color: 'var(--muted)', textTransform: 'capitalize' }}>{user.rol?.toLowerCase()}</p>
          </div>
        )}
        <button onClick={handleLogout} aria-label="Cerrar sesión" title="Cerrar sesión" className="nav-item" style={{ width: '100%', background: 'none', border: 'none' }}>
          <LogOut size={15} />
          <span className="sidebar-item-label">Salir</span>
        </button>
      </div>
    </aside>
  )
}
