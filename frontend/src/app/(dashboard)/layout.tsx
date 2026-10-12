import Providers from '@/components/Providers'
import Sidebar from '@/components/Sidebar'
import TopBar from '@/components/TopBar'
import AuthGuard from '@/components/AuthGuard'

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
