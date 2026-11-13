'use client'
import { useState } from 'react'
import { useAuthStore, hasPermiso } from '@/features/auth'
import { useAsignaciones, useReasignar, useRepartidoresHabilitados } from '../hooks'
import type { EstadoAsignacion } from '../types'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'

const CFG: Record<EstadoAsignacion, { label: string; badge: string }> = {
  ASIGNADA:   { label: 'Asignada',   badge: 'badge-confirmado' },
  EN_RUTA:    { label: 'En ruta',    badge: 'badge-preparacion' },
  ENTREGADA:  { label: 'Entregada',  badge: 'badge-entregado' },
  FALLIDA:    { label: 'Fallida',    badge: 'badge-rechazada' },
  REASIGNADA: { label: 'Reasignada', badge: 'badge-solicitada' },
}

const FILTROS: (EstadoAsignacion | 'TODOS')[] = [
  'TODOS', 'ASIGNADA', 'EN_RUTA', 'ENTREGADA', 'FALLIDA', 'REASIGNADA',
]

// Reasignar solo tiene sentido mientras no esté finalizada o ya liberada.
const REASIGNABLE: EstadoAsignacion[] = ['ASIGNADA', 'EN_RUTA', 'FALLIDA']

export default function EstadosRepartoPanel() {
  const user = useAuthStore(s => s.user)
  const puedeGestionar = hasPermiso(user, 'delivery:asignaciones-gestionar')

  const [filtro, setFiltro] = useState<EstadoAsignacion | 'TODOS'>('TODOS')
  const { data: asignaciones = [], isLoading, isError } = useAsignaciones(
    filtro === 'TODOS' ? {} : { estado: filtro },
  )
  const { data: habilitados = [] } = useRepartidoresHabilitados()
  const reasignar = useReasignar()

  const [reasignando, setReasignando] = useState<number | null>(null)
  const [destino, setDestino] = useState<number | ''>('')

  const confirmarReasignar = (pedidoClienteId: number) => {
    if (destino === '') return
    reasignar.mutate(
      { pedidoClienteId, repartidorId: destino },
      {
        onSuccess: () => {
          setReasignando(null)
          setDestino('')
        },
      },
    )
  }

  return (
    <section style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <h2 style={{ fontSize: 16, fontWeight: 800 }}>Estados de reparto</h2>

      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        {FILTROS.map(e => (
          <button
            key={e}
            onClick={() => setFiltro(e)}
            style={{
              padding: '8px 14px', borderRadius: 10, fontSize: 12, fontWeight: 600, cursor: 'pointer', border: 'none',
              background: filtro === e ? 'var(--yellow)' : 'var(--surface)',
              color: filtro === e ? '#000' : 'var(--muted)',
              outline: filtro !== e ? '1px solid var(--border)' : 'none',
            }}
          >
            {e === 'TODOS' ? 'Todas' : CFG[e].label}
          </button>
        ))}
      </div>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {isLoading ? (
          <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Cargando…</p>
        ) : isError ? (
          <p style={{ padding: 32, textAlign: 'center', color: 'var(--error)' }}>No se pudieron cargar las asignaciones</p>
        ) : asignaciones.length === 0 ? (
          <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Sin asignaciones</p>
        ) : (
          <div className="table-scroll">
            <table className="tbl">
              <thead>
                <tr>{['Pedido', 'Cliente', 'Dirección', 'Repartidor', 'Estado', 'Asignado', 'Acciones'].map(h => <th key={h}>{h}</th>)}</tr>
              </thead>
              <tbody>
                {asignaciones.map(a => {
                  const editando = reasignando === a.pedidoClienteId
                  const puedeReasignar = puedeGestionar && REASIGNABLE.includes(a.estado)
                  return (
                    <tr key={a.asignacionId}>
                      <td style={{ fontWeight: 700, color: 'var(--fuchsia)', fontSize: 12 }}>#{a.codigo ?? a.pedidoClienteId}</td>
                      <td style={{ fontWeight: 600 }}>{a.clienteNombre ?? '—'}</td>
                      <td style={{ fontSize: 12, color: 'var(--muted)', maxWidth: 200 }}>{a.direccion ?? '—'}</td>
                      <td style={{ color: 'var(--muted)' }}>#{a.repartidorId}</td>
                      <td><span className={`badge ${CFG[a.estado]?.badge}`}>{CFG[a.estado]?.label ?? a.estado}</span></td>
                      <td style={{ fontSize: 11, color: 'var(--muted)' }}>
                        {a.asignadoAt ? format(new Date(a.asignadoAt), 'dd/MM/yy HH:mm', { locale: es }) : '—'}
                        {a.asignadoPor ? <><br /><span style={{ color: 'var(--dim)' }}>{a.asignadoPor}</span></> : null}
                      </td>
                      <td>
                        {puedeReasignar && !editando && (
                          <button
                            className="btn-ghost"
                            style={{ padding: '6px 12px' }}
                            onClick={() => { setReasignando(a.pedidoClienteId); setDestino('') }}
                          >
                            Reasignar
                          </button>
                        )}
                        {editando && (
                          <div className="inline-control" style={{ flexWrap: 'wrap' }}>
                            <select
                              className="input"
                              style={{ maxWidth: 180 }}
                              value={destino}
                              onChange={e => setDestino(e.target.value === '' ? '' : Number(e.target.value))}
                            >
                              <option value="">Repartidor…</option>
                              {habilitados.map(r => (
                                <option key={r.id} value={r.id}>#{r.id} (usuario {r.usuarioId})</option>
                              ))}
                            </select>
                            <button
                              className="btn-fuchsia"
                              onClick={() => confirmarReasignar(a.pedidoClienteId)}
                              disabled={destino === '' || reasignar.isPending}
                            >
                              Confirmar
                            </button>
                            <button
                              className="btn-ghost"
                              style={{ padding: '6px 10px' }}
                              onClick={() => { setReasignando(null); setDestino('') }}
                            >
                              Cancelar
                            </button>
                          </div>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  )
}
