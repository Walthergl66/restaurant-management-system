'use client'
import { Bell } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { getGreeting, getTurno } from '@/lib/utils'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'

export default function TopBar() {
  const { user } = useAuthStore()
  const now = new Date()

  return (
    <header style={{
      borderBottom: '1px solid var(--border)',
      padding: '16px 24px',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      background: 'var(--bg)', flexShrink: 0,
    }}>
      <div>
        <h1 style={{ fontSize: 22, fontWeight: 800, color: 'var(--text)' }}>
          {getGreeting()},{' '}
          <span style={{ color: 'var(--yellow)' }}>{user?.username ?? 'equipo'}</span>
        </h1>
        <p style={{ fontSize: 13, color: 'var(--muted)', marginTop: 2 }}>
          {format(now, "EEEE, d 'de' MMMM", { locale: es })} · {getTurno()}
        </p>
      </div>
      <button
        style={{
          width: 36, height: 36, borderRadius: '50%', background: 'var(--surface-2)',
          border: '1px solid var(--border)', display: 'flex', alignItems: 'center',
          justifyContent: 'center', cursor: 'pointer', position: 'relative',
        }}
        aria-label="Notificaciones"
      >
        <Bell size={17} color="var(--muted)" />
        <span style={{
          position: 'absolute', top: 7, right: 7, width: 8, height: 8,
          borderRadius: '50%', background: 'var(--fuchsia)',
        }} />
      </button>
    </header>
  )
}
