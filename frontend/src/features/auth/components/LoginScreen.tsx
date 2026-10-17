import Image from 'next/image'
import Providers from '@/shared/layout/Providers'
import LoginForm from './LoginForm'

export default function LoginScreen() {
  return (
    <Providers>
      {/* capa de apertura: solo el logo, luego se abre como libreta */}
      <div className="login-intro" aria-hidden="true">
        <Image
          src="/Estacion-Burger-Logo.png"
          alt=""
          width={1678}
          height={937}
          className="login-intro__logo"
        />
      </div>
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