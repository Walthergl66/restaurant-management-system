'use client'
import { useMemo, useState } from 'react'
import { X, Check, Eye, EyeOff } from 'lucide-react'
import { useCrearUsuario, useActualizarUsuario, usePermisos, useTaxonomiaRoles } from '../hooks'
import type { RolDto, UsuarioAdminResponse } from '../types'

export default function UsuarioModal({ editing, roles, onClose }: { editing: UsuarioAdminResponse | null; roles: RolDto[]; onClose: () => void }) {
  const { data: taxonomia } = useTaxonomiaRoles()

  /* El panel solo asigna los roles marcados como asignables (lo decide la
   * base de datos: ADMIN es una cuenta única y CLIENTE/DELIVERY nacen por
   * auto-registro). Al editar, el rol actual del usuario siempre está
   * disponible aunque no sea asignable: es su rol y no se le puede quitar. */
  const rolesSeleccionables = roles.filter(r => r.asignablePorAdmin || r.codigo === editing?.rol)
  const codigosSeleccionables = new Set(rolesSeleccionables.map(r => r.codigo))

  /* Los roles se muestran agrupados por categoría (administración, personal
   * interno, clientes, repartidores). */
  const categoriasConRoles = (taxonomia?.categorias ?? [])
    .map(c => ({ ...c, roles: c.roles.filter(r => codigosSeleccionables.has(r.codigo)) }))
    .filter(c => c.roles.length > 0)

  const [username, setUsername] = useState(editing?.username ?? '')
  const [nombre,   setNombre]   = useState(editing?.nombre ?? '')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [activo,   setActivo]   = useState(editing?.activo ?? true)
  const [rolCodigo, setRolCodigo] = useState(editing?.rol ?? rolesSeleccionables[0]?.codigo ?? '')

  const { data: permisosCatalogo = [] } = usePermisos()

  /* Solo los roles configurables admiten funcionalidades propias; el resto
   * (ADMIN, CLIENTE, DELIVERY) nacen con las suyas por defecto. */
  const rolActual = roles.find(r => r.codigo === rolCodigo)
  const esConfigurable = rolActual?.configurable ?? false

  /* Set editable de funcionalidades. Al cambiar de rol se re-marcan los
   * defaults del nuevo rol; al editar, si el usuario ya tiene permisos
   * explícitos se cargan esos. */
  const defaultsDeRol = (codigo: string) => roles.find(r => r.codigo === codigo)?.permisos ?? []
  const [permisos, setPermisos] = useState<string[]>(
    editing?.permisos?.length ? editing.permisos : defaultsDeRol(editing?.rol ?? rolCodigo),
  )

  /* Cambio de rol: si el set actual aún coincide con los defaults de algún
   * rol (el admin no lo personalizó), se reemplaza por los del rol nuevo.
   * Se hace en el handler, no en un efecto, para evitar renders en cascada. */
  const cambiarRol = (nuevoRol: string) => {
    const actuales = new Set(permisos)
    const esSetDe = (codigo: string) => {
      const delRol = defaultsDeRol(codigo)
      return delRol.length === permisos.length && delRol.every(p => actuales.has(p))
    }
    if (esSetDe(rolCodigo) || esSetDe(editing?.rol ?? '')) {
      setPermisos(defaultsDeRol(nuevoRol))
    }
    setRolCodigo(nuevoRol)
  }

  const togglePermiso = (codigo: string) =>
    setPermisos(lista => lista.includes(codigo) ? lista.filter(p => p !== codigo) : [...lista, codigo])

  /* Agrupa el catálogo por módulo para mostrarlo por secciones. */
  const porModulo = useMemo(() => {
    const grupos = new Map<string, { codigo: string; descripcion: string }[]>()
    for (const p of permisosCatalogo) {
      const lista = grupos.get(p.modulo) ?? []
      lista.push({ codigo: p.codigo, descripcion: p.descripcion })
      grupos.set(p.modulo, lista)
    }
    return [...grupos.entries()]
  }, [permisosCatalogo])

  const crear = useCrearUsuario({ onSaved: onClose })
  const actualizar = useActualizarUsuario({ onSaved: onClose })
  const isPending = crear.isPending || actualizar.isPending

  const guardar = () => {
    if (editing) {
      actualizar.mutate({
        id: editing.id,
        payload: esConfigurable
          ? { nombre, rolCodigo, activo, permisos }
          : { nombre, rolCodigo, activo },
      })
    } else {
      crear.mutate({
        username, nombre, password, rolCodigo,
        ...(esConfigurable ? { permisos } : {}),
      })
    }
  }

  const fields = editing
    ? [{ label: 'NOMBRE', value: nombre, set: setNombre, type: 'text' }]
    : [{ label: 'USUARIO', value: username, set: setUsername, type: 'text' }, { label: 'NOMBRE', value: nombre, set: setNombre, type: 'text' }, { label: 'CONTRASEÑA', value: password, set: setPassword, type: 'password' }]

  const muestraChecklist = esConfigurable && permisosCatalogo.length > 0

  return (
    <div className="overlay">
      <div style={{ width: '100%', maxWidth: 480, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 20, padding: 24, maxHeight: '90vh', overflowY: 'auto' }}>
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
            <select value={rolCodigo} onChange={e => cambiarRol(e.target.value)} className="input">
              {categoriasConRoles.length > 0
                ? categoriasConRoles.map(categoria => (
                    <optgroup key={categoria.codigo} label={categoria.descripcion}>
                      {categoria.roles.map(r => <option key={r.id} value={r.codigo}>{r.codigo}</option>)}
                    </optgroup>
                  ))
                : rolesSeleccionables.map(r => <option key={r.id} value={r.codigo}>{r.codigo}</option>)}
            </select>
          </div>
          {editing && (
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: 'var(--muted)', cursor: 'pointer' }}>
              <input type="checkbox" checked={activo} onChange={e => setActivo(e.target.checked)} />
              Activo
            </label>
          )}

          {muestraChecklist && (
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 8 }}>
                <label style={{ fontSize: 11, fontWeight: 700, color: 'var(--muted)', letterSpacing: 1, textTransform: 'uppercase' }}>FUNCIONALIDADES</label>
                <span style={{ fontSize: 11, color: 'var(--muted)' }}>{permisos.length} seleccionadas</span>
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {porModulo.map(([modulo, items]) => (
                  <div key={modulo}>
                    <p style={{ fontSize: 11, fontWeight: 700, textTransform: 'uppercase', color: 'var(--yellow)', marginBottom: 4 }}>{modulo}</p>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                      {items.map(p => (
                        <label key={p.codigo} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12, color: 'var(--muted)', cursor: 'pointer' }}>
                          <input
                            type="checkbox"
                            checked={permisos.includes(p.codigo)}
                            onChange={() => togglePermiso(p.codigo)}
                          />
                          {p.descripcion}
                        </label>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
        <div style={{ display: 'flex', gap: 8, marginTop: 20 }}>
          <button className="btn-yellow" style={{ flex: 1, justifyContent: 'center' }} onClick={guardar} disabled={isPending || !nombre || (!editing && !username) || (muestraChecklist && permisos.length === 0)}>
            <Check size={14} /> {isPending ? 'Guardando…' : 'Guardar'}
          </button>
          <button className="btn-ghost" onClick={onClose}>Cancelar</button>
        </div>
      </div>
    </div>
  )
}
