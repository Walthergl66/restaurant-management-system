'use client'
import { useState } from 'react'
import { X, Check, Eye, EyeOff } from 'lucide-react'
import { useCrearUsuario, useActualizarUsuario } from '../hooks'
import type { RolDto, UsuarioAdminResponse } from '../types'

export default function UsuarioModal({ editing, roles, onClose }: { editing: UsuarioAdminResponse | null; roles: RolDto[]; onClose: () => void }) {
  const [username, setUsername] = useState(editing?.username ?? '')
  const [nombre,   setNombre]   = useState(editing?.nombre ?? '')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [activo,   setActivo]   = useState(editing?.activo ?? true)
  const [rolCodigo, setRolCodigo] = useState(editing?.rol ?? roles[0]?.codigo ?? '')

  const crear = useCrearUsuario({ onSaved: onClose })
  const actualizar = useActualizarUsuario({ onSaved: onClose })
  const isPending = crear.isPending || actualizar.isPending

  const guardar = () => {
    if (editing) {
      actualizar.mutate({ id: editing.id, payload: { nombre, rolCodigo, activo } })
    } else {
      crear.mutate({ username, nombre, password, rolCodigo })
    }
  }

  const fields = editing
    ? [{ label: 'NOMBRE', value: nombre, set: setNombre, type: 'text' }]
    : [{ label: 'USUARIO', value: username, set: setUsername, type: 'text' }, { label: 'NOMBRE', value: nombre, set: setNombre, type: 'text' }, { label: 'CONTRASEÑA', value: password, set: setPassword, type: 'password' }]

  return (
    <div className="overlay">
      <div style={{ width: '100%', maxWidth: 420, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 20, padding: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20 }}>
          <h2 style={{ fontWeight: 700, fontSize: 16 }}>{editing ? 'Editar usuario' : 'Nuevo usuario'}</h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--muted)' }}><X size={18} /></button>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {fields.map(({ label, value, set, type }) => (
            <div key={label}>
              <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--muted)', letterSpacing: 1, textTransform: 'uppercase', display: 'block', marginBottom: 5 }}>{label}</label>
              <div style={{ position: 'relative' }}>
                <input
                  type={type === 'password' && showPassword ? 'text' : type}
                  value={value}
                  onChange={e => set(label === 'USUARIO' ? e.target.value.trim().toLowerCase() : e.target.value)}
                  className="input"
                  style={type === 'password' ? { paddingRight: 44 } : undefined}
                />
                {type === 'password' && (
                  <button
                    type="button"
                    onClick={() => setShowPassword(visible => !visible)}
                    aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    title={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                    style={{
                      position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      padding: 2, border: 'none', background: 'transparent',
                      color: 'var(--muted)', cursor: 'pointer',
                    }}
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                )}
              </div>
            </div>
          ))}
          <div>
            <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--muted)', letterSpacing: 1, textTransform: 'uppercase', display: 'block', marginBottom: 5 }}>ROL</label>
            <select value={rolCodigo} onChange={e => setRolCodigo(e.target.value)} className="input">
              {roles.map(r => <option key={r.id} value={r.codigo}>{r.codigo}</option>)}
            </select>
          </div>
          {editing && (
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: 'var(--muted)', cursor: 'pointer' }}>
              <input type="checkbox" checked={activo} onChange={e => setActivo(e.target.checked)} />
              Activo
            </label>
          )}
        </div>
        <div style={{ display: 'flex', gap: 8, marginTop: 20 }}>
          <button className="btn-yellow" style={{ flex: 1, justifyContent: 'center' }} onClick={guardar} disabled={isPending || !nombre || (!editing && !username)}>
            <Check size={14} /> {isPending ? 'Guardando…' : 'Guardar'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}
