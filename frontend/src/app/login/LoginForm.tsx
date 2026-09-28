'use client'
import { useState, FormEvent } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'
import Logo from '@/components/Logo'
import toast from 'react-hot-toast'

export default function LoginForm() {
  const router = useRouter()
  const { login } = useAuthStore()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading,  setLoading]  = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (!username || !password) { toast.error('Completa todos los campos'); return }
    setLoading(true)
    try {
      await login(username, password)
      toast.success('Bienvenido')
      router.push('/dashboard')
    } catch (err: any) {
      toast.error(err?.response?.data?.mensaje ?? 'Credenciales incorrectas')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ width: '100%', maxWidth: 360 }}>
      {/* logo */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: 36 }}>
        <div style={{ background: '#111', border: '2px solid var(--border)', borderRadius: 20, padding: 16, marginBottom: 16 }}>
          <Logo size={90} />
        </div>
        <h1 style={{ fontSize: 28, fontWeight: 900, color: 'var(--yellow)', letterSpacing: 1 }}>¡BIENVENIDO!</h1>
        <p style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>Sabor extremo con el flow de neón.</p>
      </div>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <div>
          <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--muted)', letterSpacing: 1, textTransform: 'uppercase', display: 'block', marginBottom: 6 }}>
            EMAIL O USUARIO
          </label>
          <input
            type="text"
            value={username}
            onChange={e => setUsername(e.target.value)}
            placeholder="tu.usuario@estacion.com"
            className="input"
            style={{ borderColor: 'var(--yellow)' }}
            autoComplete="username"
          />
        </div>

        <div>
          <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--muted)', letterSpacing: 1, textTransform: 'uppercase', display: 'block', marginBottom: 6 }}>
            CONTRASEÑA
          </label>
          <input
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            placeholder="••••••••"
            className="input"
            style={{ borderColor: 'var(--yellow)' }}
            autoComplete="current-password"
          />
          <div style={{ textAlign: 'right', marginTop: 6 }}>
            <button type="button" style={{ fontSize: 12, color: 'var(--fuchsia)', background: 'none', border: 'none', cursor: 'pointer' }}>
              ¿Olvidé mi contraseña?
            </button>
          </div>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="btn-yellow"
          style={{ width: '100%', justifyContent: 'center', marginTop: 4, padding: '14px', fontSize: 14, letterSpacing: 2 }}
        >
          {loading ? 'INICIANDO...' : 'INICIAR SESIÓN'}
        </button>
      </form>

      <p style={{ textAlign: 'center', fontSize: 11, color: 'var(--dim)', marginTop: 32 }}>
        Sistema de gestión · Estación Burger © 2022
      </p>
    </div>
  )
}
