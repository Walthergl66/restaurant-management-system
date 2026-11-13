'use client'
import { CheckCircle2, PauseCircle } from 'lucide-react'
import { useAuthStore, hasPermiso } from '@/features/auth'
import { useHabilitarRepartidor, useRepartidores, useSuspenderRepartidor } from '../hooks'
import type { EstadoRepartidor } from '../types'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'

const CFG: Record<EstadoRepartidor, { label: string; badge: string }> = {
  PENDIENTE:  { label: 'Pendiente',  badge: 'badge-solicitada' },
  HABILITADO: { label: 'Habilitado', badge: 'badge-aprobada'   },
  SUSPENDIDO: { label: 'Suspendido', badge: 'badge-rechazada'  },
}

export default function RepartidoresPanel() {
  const user = useAuthStore(s => s.user)
  const puedeGestionar = hasPermiso(user, 'delivery:repartidores-gestionar')

  const { data: repartidores = [], isLoading, isError } = useRepartidores()
  const habilitar = useHabilitarRepartidor()
  const suspender = useSuspenderRepartidor()

  return (
    <section style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <h2 style={{ fontSize: 16, fontWeight: 800 }}>Repartidores</h2>
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {isLoading ? (
          <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Cargando…</p>
        ) : isError ? (
          <p style={{ padding: 32, textAlign: 'center', color: 'var(--error)' }}>No se pudieron cargar los repartidores</p>
        ) : repartidores.length === 0 ? (
          <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Sin repartidores registrados</p>
        ) : (
          <div className="table-scroll">
            <table className="tbl">
              <thead>
                <tr>{['Repartidor', 'Usuario', 'Estado', 'Habilitado por', 'Fecha', 'Acciones'].map(h => <th key={h}>{h}</th>)}</tr>
              </thead>
              <tbody>
                {repartidores.map(r => (
                  <tr key={r.id}>
                    <td className="strong-cell">#{r.id}</td>
                    <td style={{ color: 'var(--muted)' }}>{r.usuarioId}</td>
                    <td><span className={`badge ${CFG[r.estado]?.badge}`}>{CFG[r.estado]?.label ?? r.estado}</span></td>
                    <td style={{ fontSize: 12, color: 'var(--muted)' }}>{r.habilitadoPor ?? '—'}</td>
                    <td style={{ fontSize: 11, color: 'var(--muted)' }}>
                      {r.habilitadoAt ? format(new Date(r.habilitadoAt), 'dd/MM/yy HH:mm', { locale: es }) : '—'}
                    </td>
                    <td>
                      {puedeGestionar && (
                        <div className="row-actions">
                          {r.estado !== 'HABILITADO' && (
                            <button
                              onClick={() => habilitar.mutate(r.usuarioId)}
                              disabled={habilitar.isPending}
                              className="btn-ghost"
                              style={{ display: 'inline-flex', alignItems: 'center', gap: 6, color: 'var(--success)', padding: '6px 10px' }}
                              title="Habilitar"
                            >
                              <CheckCircle2 size={14} /> Habilitar
                            </button>
                          )}
                          {r.estado === 'HABILITADO' && (
                            <button
                              onClick={() => suspender.mutate(r.usuarioId)}
                              disabled={suspender.isPending}
                              className="btn-ghost"
                              style={{ display: 'inline-flex', alignItems: 'center', gap: 6, color: 'var(--error)', padding: '6px 10px' }}
                              title="Suspender"
                            >
                              <PauseCircle size={14} /> Suspender
                            </button>
                          )}
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  )
}
