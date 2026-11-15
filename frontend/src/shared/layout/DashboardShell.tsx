'use client'

import { useCallback, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { AuthGuard } from '@/features/auth'
import Providers from './Providers'
import Sidebar from './Sidebar'
import TopBar from './TopBar'

export default function DashboardShell({ children }: { children: ReactNode }) {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(true)
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  const closeMobileMenu = useCallback(() => setMobileMenuOpen(false), [])
  const handleNavigation = useCallback(() => {
    setSidebarCollapsed(true)
    closeMobileMenu()
  }, [closeMobileMenu])

  useEffect(() => {
    if (!mobileMenuOpen) return

    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') closeMobileMenu()
    }

    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [closeMobileMenu, mobileMenuOpen])

  return (
    <Providers>
      <AuthGuard>
        <div className={`dashboard-app-shell${sidebarCollapsed ? ' sidebar-collapsed' : ''}${mobileMenuOpen ? ' sidebar-open' : ''}`}>
          <TopBar
            sidebarCollapsed={sidebarCollapsed}
            mobileMenuOpen={mobileMenuOpen}
            onToggleSidebar={() => setSidebarCollapsed(value => !value)}
            onToggleMobileMenu={() => setMobileMenuOpen(value => !value)}
          />
          {mobileMenuOpen && <button className="sidebar-backdrop" type="button" aria-label="Cerrar menú de navegación" onClick={closeMobileMenu} />}
          <div className="dashboard-workspace">
            <Sidebar onNavigate={handleNavigation} />
            <div className="dashboard-page-column">
              <main className="dashboard-main">{children}</main>
            </div>
          </div>
        </div>
      </AuthGuard>
    </Providers>
  )
}
