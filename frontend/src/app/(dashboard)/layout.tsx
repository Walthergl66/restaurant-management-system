import Providers from '@/shared/layout/Providers'
import Sidebar from '@/shared/layout/Sidebar'
import TopBar from '@/shared/layout/TopBar'
import { AuthGuard } from '@/features/auth'

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <Providers>
      <AuthGuard>
        <div style={{ display: 'flex', height: '100vh', overflow: 'hidden', background: 'var(--bg)' }}>
          <Sidebar />
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, overflow: 'hidden' }}>
            <TopBar />
            <main style={{ flex: 1, overflowY: 'auto', padding: '24px' }}>
              {children}
            </main>
          </div>
        </div>
      </AuthGuard>
    </Providers>
  )
}
