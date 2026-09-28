import Providers from '@/components/Providers'
import LoginForm from './LoginForm'

export default function LoginPage() {
  return (
    <Providers>
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16, background: 'var(--bg)' }}>
        <LoginForm />
      </div>
    </Providers>
  )
}
