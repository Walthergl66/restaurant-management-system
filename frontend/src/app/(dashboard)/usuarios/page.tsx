'use client'
import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, X, Check, Shield } from 'lucide-react'
import api from '@/lib/api'
import { queryClient } from '@/lib/queryClient'
import type { UsuarioAdminResponse, RolDto } from '@/types'
import toast from 'react-hot-toast'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'

export default function UsuariosPage() {
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing]   = useState<UsuarioAdminResponse | null>(null)
  const [tab, setTab]           = useState<'usuarios' | 'roles'>('usuarios')

  const { data: usuarios = [], isLoading } = useQuery<UsuarioAdminResponse[]>({
    queryKey: ['usuarios'],
    queryFn: async () => {
      const { data } = await api.get('/usuarios', { params: { pagina: 0, tamanio: 100 } })
      return data.content ?? data
    },
  })

  const { data: roles = [] } = useQuery<RolDto[]>({
    queryKey: ['roles'],
    queryFn: async () => { const { data } = await api.get('/usuarios/roles'); return data },
  })

  const eliminar = useMutation({
    mutationFn: (id: number) => api.delete(`/usuarios/${id}`),
    onSuccess: () => { toast.success('Usuario desactivado'); queryClient.invalidateQueries({ queryKey: ['usuarios'] }) },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
        <h1 style={{ fontSize: 20, fontWeight: 800 }}>Usuarios y Roles</h1>
        <button className="btn-fuchsia" onClick={() => { setEditing(null); setShowForm(true) }}>
          <Plus size={15} /> Nuevo usuario
        </button>
      </div>

      {/* tabs */}
      <div style={{ display: 'flex', gap: 4, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 12, padding: 4, width: 'fit-content' }}>
        {(['usuarios', 'roles'] as const).map(t => (
          <button key={t} onClick={() => setTab(t)} style={{
            padding: '8px 20px', borderRadius: 9, fontSize: 13, fontWeight: 600, cursor: 'pointer', border: 'none', textTransform: 'capitalize',
            background: tab === t ? 'var(--yellow)' : 'none',
            color: tab === t ? '#000' : 'var(--muted)',
          }}>{t}</button>
        ))}
      </div>

      {tab === 'usuarios' ? (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          {isLoading
            ? <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Cargando…</p>
            : (
              <table className="tbl">
                <thead>
                  <tr>{['Usuario', 'Nombre', 'Email', 'Rol', 'Estado', 'Creado', 'Acciones'].map(h => <th key={h}>{h}</th>)}</tr>
                </thead>
                <tbody>
                  {usuarios.map(u => (
                    <tr key={u.id}>
                      <td style={{ fontWeight: 700, color: 'var(--yellow)' }}>{u.username}</td>
                      <td>{u.nombre} {u.apellido}</td>
                      <td style={{ fontSize: 12, color: 'var(--muted)' }}>{u.email}</td>
                      <td>
                        <span style={{ fontSize: 11, background: 'rgba(245,166,35,.15)', color: 'var(--yellow)', borderRadius: 20, padding: '2px 8px', fontWeight: 600 }}>
                          {u.rol}
                        </span>
                      </td>
                      <td><span className={`badge ${u.activo ? 'badge-abierta' : 'badge-cerrada'}`}>{u.activo ? 'Activo' : 'Inactivo'}</span></td>
                      <td style={{ fontSize: 12, color: 'var(--muted)' }}>{u.creadoAt ? format(new Date(u.creadoAt), 'dd/MM/yy', { locale: es }) : '—'}</td>
                      <td>
                        <div style={{ display: 'flex', gap: 6 }}>
                          <button className="btn-ghost" style={{ padding: '6px 8px' }} onClick={() => { setEditing(u); setShowForm(true) }}><Pencil size={13} /></button>
                          <button className="btn-ghost" style={{ padding: '6px 8px', color: 'var(--error)' }} onClick={() => { if (confirm(`¿Desactivar a "${u.username}"?`)) eliminar.mutate(u.id) }}><Trash2 size={13} /></button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )
          }
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(280px,1fr))', gap: 14 }}>
          {roles.map(r => (
            <div key={r.id} className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
                <Shield size={16} color="var(--yellow)" />
                <h3 style={{ fontWeight: 700, fontSize: 14 }}>{r.nombre}</h3>
                <span style={{ marginLeft: 'auto', fontSize: 11, color: 'var(--muted)' }}>{r.permisos.length} permisos</span>
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 5 }}>
                {r.permisos.slice(0, 12).map(p => (
                  <span key={p} style={{ fontSize: 10, background: 'var(--surface-2)', color: 'var(--muted)', borderRadius: 20, padding: '2px 8px' }}>{p}</span>
                ))}
                {r.permisos.length > 12 && (
                  <span style={{ fontSize: 10, background: 'var(--surface-2)', color: 'var(--muted)', borderRadius: 20, padding: '2px 8px' }}>+{r.permisos.length - 12} más</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {showForm && <UsuarioModal editing={editing} roles={roles} onClose={() => setShowForm(false)} />}
    </div>
  )
}

function UsuarioModal({ editing, roles, onClose }: { editing: UsuarioAdminResponse | null; roles: RolDto[]; onClose: () => void }) {
  const [username, setUsername] = useState(editing?.username ?? '')
  const [nombre,   setNombre]   = useState(editing?.nombre ?? '')
  const [apellido, setApellido] = useState(editing?.apellido ?? '')
  const [email,    setEmail]    = useState(editing?.email ?? '')
  const [password, setPassword] = useState('')
  const [rol,      setRol]      = useState(editing?.rol ?? roles[0]?.nombre ?? '')

  const mutation = useMutation({
    mutationFn: (): Promise<unknown> => editing
      ? api.put(`/usuarios/${editing.id}`, { nombre, apellido, email, rol })
      : api.post('/usuarios', { username, nombre, apellido, email, password, rol }),
    onSuccess: () => {
      toast.success(editing ? 'Usuario actualizado' : 'Usuario creado')
      queryClient.invalidateQueries({ queryKey: ['usuarios'] })
      onClose()
    },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  const fields = editing
    ? [{ label: 'NOMBRE', value: nombre, set: setNombre, type: 'text' }, { label: 'APELLIDO', value: apellido, set: setApellido, type: 'text' }, { label: 'EMAIL', value: email, set: setEmail, type: 'email' }]
    : [{ label: 'USUARIO', value: username, set: setUsername, type: 'text' }, { label: 'NOMBRE', value: nombre, set: setNombre, type: 'text' }, { label: 'APELLIDO', value: apellido, set: setApellido, type: 'text' }, { label: 'EMAIL', value: email, set: setEmail, type: 'email' }, { label: 'CONTRASEÑA', value: password, set: setPassword, type: 'password' }]

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
              <input type={type} value={value} onChange={e => set(e.target.value)} className="input" />
            </div>
          ))}
          <div>
            <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--muted)', letterSpacing: 1, textTransform: 'uppercase', display: 'block', marginBottom: 5 }}>ROL</label>
            <select value={rol} onChange={e => setRol(e.target.value)} className="input">
              {roles.map(r => <option key={r.id} value={r.nombre}>{r.nombre}</option>)}
            </select>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8, marginTop: 20 }}>
          <button className="btn-yellow" style={{ flex: 1, justifyContent: 'center' }} onClick={() => mutation.mutate()} disabled={mutation.isPending}>
            <Check size={14} /> {mutation.isPending ? 'Guardando…' : 'Guardar'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}
