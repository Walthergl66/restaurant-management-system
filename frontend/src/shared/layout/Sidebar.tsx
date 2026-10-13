'use client'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import {
  LayoutDashboard, ShoppingBag, Package, Table2, DollarSign,
  Users, XCircle, ClipboardList, BarChart3, LogOut, ChefHat, Receipt,
} from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { hasPermiso } from '@/shared/lib/permissions'
import Logo from '@/shared/ui/Logo'
import toast from 'react-hot-toast'

const NAV: { href: string; label: string; icon: typeof LayoutDashboard; permisos?: string[] }[] = [
  { href: '/dashboard',   label: 'Resumen',    icon: LayoutDashboard },
  { href: '/pedidos',     label: 'Pedidos',    icon: ShoppingBag,  permisos: ['pedidos:ver'] },
  { href: '/cocina',      label: 'Cocina',     icon: ChefHat,      permisos: ['comandas:ver'] },
  { href: '/mesas',       label: 'Mesas',      icon: Table2,       permisos: ['mesas:gestionar'] },
  { href: '/productos',   label: 'Productos',  icon: Package,      permisos: ['catalogo:ver'] },
  { href: '/cuentas',     label: 'Cuentas',    icon: Receipt,      permisos: ['cuentas:ver'] },
  { href: '/caja',        label: 'Caja',       icon: DollarSign,   permisos: ['caja:movimientos'] },
  { href: '/reportes',    label: 'Reportes',   icon: BarChart3,    permisos: ['reportes:ver'] },
  { href: '/usuarios',    label: 'Usuarios',   icon: Users,        permisos: ['usuarios:ver'] },
  { href: '/anulaciones', label: 'Anulaciones',icon: XCircle,      permisos: ['anulaciones:solicitar', 'anulaciones:aprobar'] },
  { href: '/auditoria',   label: 'Auditoría',  icon: ClipboardList,permisos: ['auditoria:ver'] },
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
    <aside style={{
      width: 172, flexShrink: 0, background: 'var(--surface)',
      borderRight: '1px solid var(--border)',
      display: 'flex', flexDirection: 'column', padding: '16px 10px',
      height: '100vh', position: 'sticky', top: 0, overflowY: 'auto',
    }}>
      {/* Logo */}
      <div style={{
        display: 'flex', flexDirection: 'column',
        alignItems: 'center', padding: '12px 8px', marginBottom: 16,
      }}>
        <Logo size={128} />
        <p style={{ color: 'var(--yellow)', fontWeight: 800, fontSize: 11, letterSpacing: 3, marginTop: 4 }}>ESTACIÓN</p>
        <p style={{ color: 'var(--fuchsia)', fontWeight: 800, fontSize: 10, letterSpacing: 2 }}>BURGER</p>
      </div>

      {/* Nav */}
      <nav style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
        {nav.map(({ href, label, icon: Icon }) => {
          const active = pathname === href || (href !== '/dashboard' && pathname.startsWith(href))
          return (
            <Link key={href} href={href} className={`nav-item${active ? ' active' : ''}`}>
              <Icon size={16} strokeWidth={1.8} />
              <span>{label}</span>
            </Link>
          )
        })}
      </nav>

      {/* Footer */}
      <div style={{ borderTop: '1px solid var(--border)', paddingTop: 12, marginTop: 12 }}>
        {user && (
          <div style={{ padding: '0 4px', marginBottom: 8 }}>
            <p style={{ fontSize: 12, fontWeight: 600, color: 'var(--text)' }}>{user.username}</p>
            <p style={{ fontSize: 11, color: 'var(--muted)', textTransform: 'capitalize' }}>{user.rol?.toLowerCase()}</p>
          </div>
        )}
        <button onClick={handleLogout} className="nav-item" style={{ width: '100%', background: 'none', border: 'none' }}>
          <LogOut size={15} />
          <span>Salir</span>
        </button>
      </div>
    </aside>
  )
}
