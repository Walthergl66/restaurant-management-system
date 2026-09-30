'use client'
import { useState, FormEvent } from 'react'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '@/store/authStore'
import Logo from '@/components/Logo'
import toast from 'react-hot-toast'
import { Eye, EyeOff, LogIn } from 'lucide-react'

export default function LoginForm() {
  const router = useRouter()
  const { login } = useAuthStore()
  const [username,    setUsername]    = useState('')
  const [password,    setPassword]    = useState('')
  const [showPass,    setShowPass]    = useState(false)
  const [loading,     setLoading]     = useState(false)

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
    /* ── outer wrapper: split layout en desktop, centrado en mobile ── */
    <div style={{
      display: 'flex',
      width: '100%',
      maxWidth: 900,
      minHeight: 560,
      borderRadius: 24,
      overflow: 'hidden',
      background: 'var(--surface)',
      border: '1px solid var(--border)',
      boxShadow: '0 32px 80px rgba(0,0,0,.6)',
    }}>

      {/* ── left panel: brand (solo visible en ≥ 700px) ── */}
      <div style={{
        flex: 1,
        background: 'linear-gradient(145deg, #0d0d0d 0%, #1a1a1a 100%)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '48px 40px',
        borderRight: '1px solid var(--border)',
        position: 'relative',
        overflow: 'hidden',
      }}
        className="login-brand-panel"
      >
        {/* glow decorativo */}
        <div style={{ position:'absolute', top:'20%', left:'50%', transform:'translateX(-50%)', width:300, height:300, borderRadius:'50%', background:'radial-gradient(circle, rgba(245,166,35,.12) 0%, transparent 70%)', pointerEvents:'none' }}/>
        <div style={{ position:'absolute', bottom:'15%', left:'50%', transform:'translateX(-50%)', width:200, height:200, borderRadius:'50%', background:'radial-gradient(circle, rgba(233,30,140,.1) 0%, transparent 70%)', pointerEvents:'none' }}/>

        {/* Logo del sistema */}
        <div style={{ position:'relative', width:200, height:200, marginBottom:28 }}>
          <Logo size={200} />
        </div>

        <h2 style={{ fontSize:28, fontWeight:900, color:'var(--yellow)', letterSpacing:2, textAlign:'center', textShadow:'0 0 20px rgba(245,166,35,.4)' }}>
          ESTACIÓN BURGER
        </h2>
        <p style={{ fontSize:13, color:'var(--muted)', marginTop:8, textAlign:'center', lineHeight:1.5 }}>
          Sistema de gestión integral<br/>para tu restaurante
        </p>

        <div style={{ marginTop:40, display:'flex', flexDirection:'column', gap:10, width:'100%', maxWidth:220 }}>
          {[
            { dot:'var(--yellow)',  text:'Dashboard en tiempo real' },
            { dot:'var(--fuchsia)', text:'Gestión de pedidos y cocina' },
            { dot:'var(--success)', text:'Control de caja y reportes' },
          ].map(({ dot, text }) => (
            <div key={text} style={{ display:'flex', alignItems:'center', gap:10 }}>
              <span style={{ width:8, height:8, borderRadius:'50%', background:dot, flexShrink:0, boxShadow:`0 0 6px ${dot}` }}/>
              <span style={{ fontSize:12, color:'var(--muted)' }}>{text}</span>
            </div>
          ))}
        </div>
      </div>

      {/* ── right panel: form ── */}
      <div style={{
        width: '100%',
        maxWidth: 420,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        padding: '48px 40px',
        background: 'var(--bg)',
      }}>

        {/* logo pequeño solo en mobile (cuando el panel izq está oculto) */}
        <div style={{ display:'flex', justifyContent:'center', marginBottom:8 }} className="login-mobile-logo">
          <Logo size={80} />
        </div>

        <h1 style={{ fontSize:26, fontWeight:900, color:'var(--yellow)', marginBottom:4, textAlign:'center' }}>
          ¡BIENVENIDO!
        </h1>
        <p style={{ fontSize:13, color:'var(--muted)', textAlign:'center', marginBottom:36 }}>
          Sabor extremo con el flow de neón.
        </p>

        <form onSubmit={handleSubmit} style={{ display:'flex', flexDirection:'column', gap:20 }}>

          {/* username */}
          <div>
            <label style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:8 }}>
              Email o usuario
            </label>
            <input
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              placeholder="carlos.estacion@burger.com"
              className="input"
              style={{ fontSize:14, padding:'13px 16px', borderColor: username ? 'var(--yellow)' : 'var(--border)' }}
              autoComplete="username"
              autoFocus
            />
          </div>

          {/* password */}
          <div>
            <label style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:8 }}>
              Contraseña
            </label>
            <div style={{ position:'relative' }}>
              <input
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
                style={{ position:'absolute', right:14, top:'50%', transform:'translateY(-50%)', background:'none', border:'none', cursor:'pointer', color:'var(--muted)', display:'flex', alignItems:'center' }}
                tabIndex={-1}
              >
                {showPass ? <EyeOff size={17}/> : <Eye size={17}/>}
              </button>
            </div>
            <div style={{ textAlign:'right', marginTop:6 }}>
              <button type="button" style={{ fontSize:12, color:'var(--fuchsia)', background:'none', border:'none', cursor:'pointer' }}>
                ¿Olvidé mi contraseña?
              </button>
            </div>
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
              : <><LogIn size={16}/> INICIAR SESIÓN</>
            }
          </button>

          <p style={{ fontSize:11, color:'var(--dim)', textAlign:'center', marginTop:8 }}>
            ¿No tienes acceso? Contacta al administrador del sistema.
          </p>
        </form>

        <p style={{ fontSize:11, color:'var(--dim)', textAlign:'center', marginTop:40 }}>
          Sistema de gestión · Estación Burger © 2022
        </p>
      </div>
    </div>
  )
}
