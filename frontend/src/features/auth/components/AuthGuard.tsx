'use client'
import { useEffect, useSyncExternalStore } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '../store'
import { subscribeSession } from '@/shared/api/session'

const emptySubscribe = () => () => {}

export default function AuthGuard({ children }: { children: React.ReactNode }) {
  const { status, sessionError, initializeSession, syncSession, sessionUnavailable, fetchMe, logout } = useAuthStore()
  const router = useRouter()
  const mounted = useSyncExternalStore(emptySubscribe, () => true, () => false)

  useEffect(() => {
    const unsubscribe = subscribeSession(() => { void syncSession() }, sessionUnavailable)
    void initializeSession()
    return unsubscribe
  }, [initializeSession, syncSession, sessionUnavailable])

  useEffect(() => {
    if (mounted && status === 'anonymous') router.replace('/login')
  }, [mounted, status, router])

  if (!mounted || status === 'unknown' || status === 'checking') {
    return <div role="status" style={{ padding: 32, color: 'var(--muted)' }}>Comprobando sesión…</div>
  }
  if (status === 'error') {
    return <div style={{ padding: 32, maxWidth: 520, margin: 'auto' }}>
      <p role="alert">{sessionError}</p>
      <div style={{ display: 'flex', gap: 12, marginTop: 16 }}>
        <button className="btn-yellow" onClick={() => { void fetchMe() }}>Reintentar</button>
        <button className="btn-ghost" onClick={() => { void logout() }}>Volver a iniciar sesión</button>
      </div>
    </div>
  }
  if (status !== 'authenticated') return null
  return <>{children}</>
}
