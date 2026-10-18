'use client'
import { useState } from 'react'
import { AlertCircle, CheckCircle2, Pencil, Plus, Power, RefreshCw, Users } from 'lucide-react'
import { useCambiarHabilitacionMesa, useMesas } from '../hooks'
import type { Mesa } from '../types'
import MesaModal from './MesaModal'
import ConfirmDialog from '@/shared/ui/ConfirmDialog'
import { hasPermiso, useAuthStore } from '@/features/auth'

export default function MesasScreen() {
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState<Mesa | null>(null)
  const [alternar, setAlternar] = useState<Mesa | null>(null)
  const user = useAuthStore(state => state.user)
  const { data: mesas = [], isLoading, isError, refetch } = useMesas()
  const cambiarHabilitacion = useCambiarHabilitacionMesa()
  const puedeGestionar = hasPermiso(user, 'mesas:gestionar')

  const habilitadas = mesas.filter(mesa => mesa.activo).length
  const kpis = [
    { label: 'Total', count: mesas.length, color: 'var(--text)' },
    { label: 'Habilitadas', count: habilitadas, color: '#22c55e' },
    { label: 'Deshabilitadas', count: mesas.length - habilitadas, color: 'var(--muted)' },
  ]

  const abrirFormulario = (mesa: Mesa | null) => {
    setEditing(mesa)
    setShowForm(true)
  }

  const alternarHabilitacion = (mesa: Mesa) => setAlternar(mesa)

  if (!puedeGestionar) {
    return (
      <div className="card" style={{ padding: 40, textAlign: 'center' }}>
        <AlertCircle size={28} color="var(--error)" style={{ margin: '0 auto 10px' }} />
        <p style={{ fontWeight: 700 }}>Acceso restringido</p>
        <p style={{ color: 'var(--muted)', fontSize: 12, marginTop: 6 }}>La administración del catálogo de mesas requiere permisos de gestión.</p>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
        <div>
          <h1 style={{ fontSize: 20, fontWeight: 800 }}>Mesas</h1>
          <p style={{ color: 'var(--muted)', fontSize: 12, marginTop: 4 }}>Catálogo de mesas disponibles para nuevos pedidos</p>
        </div>
        <button className="btn-fuchsia" onClick={() => abrirFormulario(null)}><Plus size={15} /> Nueva mesa</button>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 12 }}>
        {kpis.map(kpi => (
          <div key={kpi.label} className="card" style={{ textAlign: 'center', padding: '16px 12px' }}>
            <p style={{ fontSize: 32, fontWeight: 900, color: kpi.color }}>{kpi.count}</p>
            <p style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>{kpi.label}</p>
          </div>
        ))}
      </div>

      {isLoading && <div className="card" style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Cargando catálogo de mesas…</div>}

      {!isLoading && isError && (
        <div className="card" style={{ padding: 32, textAlign: 'center' }}>
          <AlertCircle size={28} color="var(--error)" style={{ margin: '0 auto 10px' }} />
          <p style={{ fontWeight: 700 }}>No se pudo cargar el catálogo</p>
          <p style={{ color: 'var(--muted)', fontSize: 12, margin: '6px 0 14px' }}>Comprueba la conexión con el backend e inténtalo nuevamente.</p>
          <button className="btn-ghost" onClick={() => refetch()}><RefreshCw size={14} /> Reintentar</button>
        </div>
      )}

      {!isLoading && !isError && mesas.length === 0 && (
        <div className="card" style={{ padding: 40, textAlign: 'center' }}>
          <p style={{ fontWeight: 700 }}>Todavía no hay mesas registradas</p>
          <p style={{ color: 'var(--muted)', fontSize: 12, marginTop: 6 }}>Crea la primera mesa para comenzar a construir el catálogo.</p>
        </div>
      )}

      {!isLoading && !isError && mesas.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(210px, 1fr))', gap: 12 }}>
          {mesas.map(mesa => (
            <article key={mesa.id} style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 16, padding: 16, display: 'flex', flexDirection: 'column', gap: 14, opacity: mesa.activo ? 1 : 0.7 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 8 }}>
                <div>
                  <p style={{ fontSize: 20, fontWeight: 900 }}>Mesa {mesa.numero}</p>
                  <p style={{ fontSize: 11, color: 'var(--muted)', marginTop: 2 }}>{mesa.ubicacion || 'Sin ubicación'}</p>
                </div>
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, borderRadius: 20, padding: '4px 8px', fontSize: 11, fontWeight: 700, color: mesa.activo ? '#22c55e' : 'var(--muted)', background: mesa.activo ? 'rgba(34,197,94,.12)' : 'var(--surface-2)' }}>
                  <CheckCircle2 size={11} /> {mesa.activo ? 'Habilitada' : 'Deshabilitada'}
                </span>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--muted)' }}>
                <Users size={14} /><span style={{ fontSize: 12 }}>Capacidad: {mesa.capacidad} personas</span>
              </div>

              <div style={{ display: 'flex', gap: 6, marginTop: 'auto' }}>
                <button className="btn-ghost" style={{ flex: 1, justifyContent: 'center', fontSize: 11, padding: '7px 8px' }} onClick={() => abrirFormulario(mesa)}><Pencil size={12} /> Editar</button>
                <button className="btn-ghost" style={{ flex: 1, justifyContent: 'center', fontSize: 11, padding: '7px 8px', color: mesa.activo ? 'var(--error)' : '#22c55e' }} disabled={cambiarHabilitacion.isPending} onClick={() => alternarHabilitacion(mesa)}>
                  <Power size={12} /> {mesa.activo ? 'Deshabilitar' : 'Habilitar'}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}

      {showForm && <MesaModal editing={editing} onClose={() => setShowForm(false)} />}

      {alternar && (
        <ConfirmDialog
          title={`${alternar.activo ? 'Deshabilitar' : 'Habilitar'} mesa ${alternar.numero}`}
          message={`¿Deseas ${alternar.activo ? 'deshabilitar' : 'habilitar'} la mesa ${alternar.numero}?`}
          confirmText={alternar.activo ? 'Deshabilitar' : 'Habilitar'}
          variant={alternar.activo ? 'danger' : 'primary'}
          pending={cambiarHabilitacion.isPending}
          onConfirm={() => cambiarHabilitacion.mutate(
            { id: alternar.id, habilitada: !alternar.activo },
            { onSuccess: () => setAlternar(null) },
          )}
          onClose={() => setAlternar(null)}
        />
      )}
    </div>
  )
}
