'use client'
import { useState, FormEvent } from 'react'
import axios from 'axios'
import { useRouter } from 'next/navigation'
import Image from 'next/image'
import { useAuthStore } from '../store'
import { errMsg } from '@/shared/lib/utils'
import toast from 'react-hot-toast'
import { Eye, EyeOff, UserRound, LockKeyhole } from 'lucide-react'

export default function LoginForm() {
  const router = useRouter()
  const { login } = useAuthStore()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPass, setShowPass] = useState(false)
  const [loading,  setLoading]  = useState(false)
  const [error,    setError]    = useState('')

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    if (!username || !password) { setError('Completa todos los campos'); return }
    setLoading(true)
    try {
      await login(username, password)
      toast.success('Bienvenido')
      router.push('/dashboard')
    } catch (err: unknown) {
      if (!axios.isCancel(err)) {
        const temporary = axios.isAxiosError(err) && (!err.response || err.response.status >= 500)
        setError(temporary ? 'No se pudo iniciar sesión. Revisa la conexión e intenta nuevamente.' : errMsg(err, 'Credenciales incorrectas'))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    /* ── outer wrapper: split layout en desktop, centrado en mobile ── */
    <div className="login-shell" style={{
      display: 'flex',
      width: '100%',
      maxWidth: 900,
      minHeight: 560,
      borderRadius: 0,
      overflow: 'hidden',
      background: 'var(--surface)',
      border: '1px solid var(--border)',
      boxShadow: '0 32px 80px rgba(0,0,0,.6)',
    }}>

      {/* ── left panel: marca protagonista (oculto en mobile) ── */}
      <div
        className="login-brand-panel"
        style={{
          background: 'linear-gradient(180deg, rgba(10,10,10,.1), rgba(10,10,10,.24)), url("/login-counter.png") center / cover',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '48px 40px',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <div className="login-brand-lockup" style={{ width:400, maxWidth:'100%', position:'relative' }}>
          <Image
            src="/login-logo-propuesta.png"
            alt="Estación Burger"
            width={1678}
            height={937}
            className="login-logo"
            style={{ width:'100%', height:'auto', display:'block' }}
            priority
          />
        </div>


      </div>

      {/* ── right panel: formulario minimalista ── */}
      <div
        className="login-form-panel"
        style={{
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'center',
          background: '#171412',
        }}
      >
        <div className="login-form-body" style={{ padding: '48px 40px' }}>

        <div className="login-mobile-reveal-top">
        <div>
        <h1 className="login-title">Bienvenido</h1>
        </div>
        </div>

        {/* logo pequeño solo en mobile (cuando el panel izq está oculto) */}
        <div style={{ display:'flex', justifyContent:'center', margin:'0 0 12px' }} className="login-mobile-logo">
          <div style={{ width:320, maxWidth:'100%' }}>
            <Image
              src="/Estacion-Burger-Logo.png"
              alt="Estación Burger"
              width={1678}
              height={937}
              style={{ width:'100%', height:'auto', display:'block' }}
              priority
            />
          </div>
        </div>

        <div className="login-mobile-reveal-bottom">
        <div>

        <form onSubmit={handleSubmit} style={{ display:'flex', flexDirection:'column', gap:18 }}>

          {/* username */}
          <div>
            <label htmlFor="username" style={{ fontSize:12, fontWeight:700, color:'var(--text)', letterSpacing:.5, display:'block', marginBottom:8 }}>
              Correo o nombre de usuario
            </label>
            <div className="login-field"><UserRound className="login-field-icon" aria-hidden="true" /><input
              id="username"
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="Tu correo o nombre de usuario"
              className="input"
              style={{ fontSize:14, padding:'13px 16px', borderColor: username ? 'var(--yellow)' : 'var(--border)' }}
              autoComplete="username"
              autoFocus
            /></div>
          </div>

          {/* password */}
          <div>
            <label htmlFor="password" style={{ fontSize:12, fontWeight:700, color:'var(--text)', letterSpacing:.5, display:'block', marginBottom:8 }}>
              Contraseña
            </label>
            <div style={{ position:'relative' }}>
              <LockKeyhole className="login-field-icon" aria-hidden="true" />
              <input
                id="password"
                type={showPass ? 'text' : 'password'}
                value={password}
                onChange={e => setPassword(e.target.value)}
                placeholder="••••••••"
                className="input"
                style={{ fontSize:14, padding:'13px 48px 13px 16px', borderColor: password ? 'var(--yellow)' : 'var(--border)' }}
                autoComplete="current-password"
              />
              <button
                type="button"
                onClick={() => setShowPass(!showPass)}
                aria-label={showPass ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                aria-pressed={showPass}
                style={{ position:'absolute', right:8, top:'50%', transform:'translateY(-50%)', background:'none', border:'none', cursor:'pointer', color:'var(--text)', display:'flex', alignItems:'center', justifyContent:'center', width:40, height:40, borderRadius:8 }}
              >
                {showPass ? <EyeOff size={17}/> : <Eye size={17}/>}
              </button>
            </div>
          </div>

          {/* error inline */}
          {error && (
            <p role="alert" style={{ fontSize:12, color:'var(--error)', margin:0, textAlign:'center' }}>
              {error}
            </p>
          )}

          {/* forgot password */}
          <div style={{ textAlign:'right', marginTop:-12, marginBottom:-8 }}>
            <button
              type="button"
              onClick={() => toast('Contacta a un administrador para restablecer tu contraseña')}
              className="login-recovery-link"
              style={{ background:'none', border:'none', cursor:'pointer', color:'var(--fuchsia)', fontSize:13, fontWeight:700, minHeight:44, padding:'8px 4px' }}
            >
              ¿Olvidaste la contraseña?
            </button>
          </div>

          {/* submit */}
          <button
            type="submit"
            disabled={loading}
            className="btn-yellow"
            style={{ width:'100%', justifyContent:'center', padding:'14px', fontSize:14, letterSpacing:2, marginTop:4, borderRadius:14 }}
          >
            {loading
              ? <><span style={{ width:16, height:16, border:'2px solid #00000040', borderTop:'2px solid #000', borderRadius:'50%', display:'inline-block', animation:'spin .7s linear infinite' }}/> Iniciando...</>
              : 'Iniciar sesión'
            }
          </button>
        </form>

        </div>
        </div>
        </div>
      </div>
    </div>
  )
}
