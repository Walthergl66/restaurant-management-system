import Providers from '@/shared/layout/Providers'
import Sidebar from '@/shared/layout/Sidebar'
import TopBar from '@/shared/layout/TopBar'
import { AuthGuard } from '@/features/auth'

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <Providers>
      <AuthGuard>
        <div className="dashboard-app-shell">
          <TopBar />
          <div className="dashboard-workspace">
            <Sidebar />
            <div className="dashboard-page-column">
            <main style={{ flex: 1, overflowY: 'auto', padding: '24px' }}>
              {children}
            </main>
          </div>
          </div>
        </div>
      </AuthGuard>
    </Providers>
  )
}
