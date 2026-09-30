import Providers from '@/components/Providers'
import LoginForm from './LoginForm'

export default function LoginPage() {
  return (
    <Providers>
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px 16px',
        background: 'var(--bg)',
        /* sutil patrón de puntos en el fondo */
        backgroundImage: 'radial-gradient(circle, #1e1e1e 1px, transparent 1px)',
        backgroundSize: '28px 28px',
      }}>
        <LoginForm />
      </div>
    </Providers>
  )
}
