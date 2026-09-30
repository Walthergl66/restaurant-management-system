'use client'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import {
  LayoutDashboard, ShoppingBag, Package, Table2, DollarSign,
  Users, XCircle, ClipboardList, BarChart3, LogOut, ChefHat, Receipt,
} from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import Logo from './Logo'
import toast from 'react-hot-toast'

const NAV = [
  { href: '/dashboard',   label: 'Resumen',    icon: LayoutDashboard },
  { href: '/pedidos',     label: 'Pedidos',    icon: ShoppingBag },
  { href: '/cocina',      label: 'Cocina',     icon: ChefHat },
  { href: '/mesas',       label: 'Mesas',      icon: Table2 },
  { href: '/productos',   label: 'Productos',  icon: Package },
  { href: '/cuentas',     label: 'Cuentas',    icon: Receipt },
  { href: '/caja',        label: 'Caja',       icon: DollarSign },
  { href: '/reportes',    label: 'Reportes',   icon: BarChart3 },
  { href: '/usuarios',    label: 'Usuarios',   icon: Users },
  { href: '/anulaciones', label: 'Anulaciones',icon: XCircle },
  { href: '/auditoria',   label: 'Auditoría',  icon: ClipboardList },
]

export default function Sidebar() {
  const pathname = usePathname()
  const router   = useRouter()
  const { user, logout } = useAuthStore()

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
        display: 'flex', justifyContent: 'center', marginBottom: 16,
      }}>
        <Logo size={112} />
      </div>

      {/* Nav */}
      <nav style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 2 }}>
        {NAV.map(({ href, label, icon: Icon }) => {
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
        <div style={{ padding: '0 4px', marginBottom: 10 }}>
          <p style={{ fontSize: 10, color: 'var(--dim)', textTransform: 'uppercase', letterSpacing: 1 }}>Sucursal activa</p>
          <p style={{ fontSize: 12, fontWeight: 700, color: 'var(--text)', marginTop: 2 }}>Estación Centro</p>
          <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginTop: 3 }}>
            <span style={{ width: 7, height: 7, borderRadius: '50%', background: 'var(--success)' }} />
            <span style={{ fontSize: 11, color: 'var(--success)' }}>Operando</span>
          </div>
        </div>
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
