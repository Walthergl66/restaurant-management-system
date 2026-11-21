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
  { href: '/comandas', group: 'Operación',    label: 'Comandas',   icon: ChefHat,      permisos: ['comandas:ver'] },
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

type SidebarProps = {
  onNavigate?: () => void
  onExpand?: () => void
  onCollapse?: () => void
}

export default function Sidebar({ onNavigate, onExpand, onCollapse }: SidebarProps) {
  const pathname = usePathname()
  const { user } = useAuthStore()

  const nav = NAV.filter((item) => !item.permisos || hasPermiso(user, ...item.permisos))

  return (
    <aside
      className="sidebar"
      id="primary-sidebar"
      aria-label="Navegación principal"
      onPointerEnter={event => {
        if (event.pointerType === 'mouse' && window.matchMedia('(min-width: 901px)').matches) onExpand?.()
      }}
      onPointerLeave={event => {
        if (event.pointerType === 'mouse' && window.matchMedia('(min-width: 901px)').matches) onCollapse?.()
      }}
      onFocusCapture={() => {
        if (window.matchMedia('(min-width: 901px)').matches) onExpand?.()
      }}
      onBlurCapture={event => {
        if (!event.currentTarget.contains(event.relatedTarget as Node | null) && window.matchMedia('(min-width: 901px)').matches) onCollapse?.()
      }}
    >
      <nav className="sidebar-nav" aria-label="Navegación principal">
        {(['Operación', 'Administración', 'Control'] as const).map((group) => {
          const items = nav.filter((item) => item.group === group)
          if (!items.length) return null
          return <div className="sidebar-group" key={group}>
            <p className="sidebar-group-label">{group}</p>
            {items.map(({ href, label, icon: Icon }) => {
              const active = pathname === href || (href !== '/dashboard' && pathname.startsWith(`${href}/`))
              return <Link key={href} href={href} className={`nav-item${active ? ' active' : ''}`} aria-current={active ? 'page' : undefined} aria-label={label} title={label} onClick={onNavigate}>
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
