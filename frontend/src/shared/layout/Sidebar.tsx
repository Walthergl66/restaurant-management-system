'use client'
import Link from 'next/link'
import { usePathname } from 'next/navigation'
import {
  LayoutDashboard, ShoppingBag, Package, Table2, DollarSign,
  Users, XCircle, ClipboardList, BarChart3, ChefHat, Receipt, Truck,
} from 'lucide-react'
import { useAuthStore } from '@/features/auth'
import { hasPermiso } from '@/shared/lib/permissions'

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
  const { user } = useAuthStore()

  const nav = NAV.filter((item) => !item.permisos || hasPermiso(user, ...item.permisos))

  return (
    <aside className="sidebar">
      <div className="sidebar-brand" aria-label="Estación Burger">
        <div className="sidebar-ticket"><span className="sidebar-ticket-name">Estación Burger</span><span className="sidebar-ticket-short" aria-hidden="true">EB</span></div>
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

    </aside>
  )
}
