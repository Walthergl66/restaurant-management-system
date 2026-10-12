'use client'
import { useState } from 'react'
import { Plus, Pencil, Trash2, Shield } from 'lucide-react'
import { useUsuarios, useRoles, useEliminarUsuario } from '../hooks'
import type { UsuarioAdminResponse } from '../types'
import UsuarioModal from './UsuarioModal'

export default function UsuariosScreen() {
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing]   = useState<UsuarioAdminResponse | null>(null)
  const [tab, setTab]           = useState<'usuarios' | 'roles'>('usuarios')

  const { data: usuarios = [], isLoading } = useUsuarios()
  const { data: roles = [] } = useRoles()
  const eliminar = useEliminarUsuario()

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
                  <tr>{['Usuario', 'Nombre', 'Rol', 'Estado', 'Acciones'].map(h => <th key={h}>{h}</th>)}</tr>
                </thead>
                <tbody>
                  {usuarios.map(u => (
                    <tr key={u.id}>
                      <td style={{ fontWeight: 700, color: 'var(--yellow)' }}>{u.username}</td>
                      <td>{u.nombre}</td>
                      <td>
                        <span style={{ fontSize: 11, background: 'rgba(245,166,35,.15)', color: 'var(--yellow)', borderRadius: 20, padding: '2px 8px', fontWeight: 600 }}>
                          {u.rol}
                        </span>
                      </td>
                      <td><span className={`badge ${u.activo ? 'badge-abierta' : 'badge-cerrada'}`}>{u.activo ? 'Activo' : 'Inactivo'}</span></td>
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
                <h3 style={{ fontWeight: 700, fontSize: 14 }}>{r.codigo}</h3>
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
