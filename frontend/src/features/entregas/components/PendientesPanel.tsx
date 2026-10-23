'use client'
import { useMemo, useState } from 'react'
import { useAuthStore, hasPermiso } from '@/features/auth'
import {
  useAsignar,
  useAsignarLote,
  usePedidosPendientes,
  useRepartidoresHabilitados,
} from '../hooks'

export default function PendientesPanel() {
  const user = useAuthStore(s => s.user)
  const puedeGestionar = hasPermiso(user, 'delivery:asignaciones-gestionar')

  const { data: pedidos = [], isLoading, isError } = usePedidosPendientes()
  const { data: habilitados = [] } = useRepartidoresHabilitados()
  const asignar = useAsignar()
  const asignarLote = useAsignarLote()

  const [seleccion, setSeleccion] = useState<number[]>([])
  const [repartidorId, setRepartidorId] = useState<number | ''>('')

  const idsDisponibles = useMemo(() => pedidos.map(p => p.pedidoClienteId), [pedidos])
  const todosSeleccionados = seleccion.length > 0 && seleccion.length === idsDisponibles.length

  const toggle = (id: number) =>
    setSeleccion(prev => (prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id]))

  const toggleTodos = () =>
    setSeleccion(todosSeleccionados ? [] : idsDisponibles)

  const asignarUno = (pedidoClienteId: number) => {
    if (repartidorId === '') return
    asignar.mutate({ pedidoClienteId, repartidorId })
  }

  const asignarVarios = () => {
    if (repartidorId === '' || seleccion.length === 0) return
    asignarLote.mutate(
      { pedidoClienteIds: seleccion, repartidorId },
      { onSuccess: () => setSeleccion([]) },
    )
  }

  const sinRepartidor = repartidorId === ''

  return (
    <section style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <h2 style={{ fontSize: 16, fontWeight: 800 }}>Pedidos listos para asignar</h2>

      {puedeGestionar && (
        <div className="inline-control" style={{ flexWrap: 'wrap', alignItems: 'center' }}>
          <select
            className="input"
            style={{ maxWidth: 240 }}
            value={repartidorId}
            onChange={e => setRepartidorId(e.target.value === '' ? '' : Number(e.target.value))}
          >
            <option value="">Repartidor habilitado…</option>
            {habilitados.map(r => (
              <option key={r.id} value={r.id}>Repartidor #{r.id} (usuario {r.usuarioId})</option>
            ))}
          </select>
          <button
            className="btn-fuchsia"
            onClick={asignarVarios}
            disabled={sinRepartidor || seleccion.length === 0 || asignarLote.isPending}
          >
            Asignar varios ({seleccion.length})
          </button>
          {habilitados.length === 0 && (
            <span style={{ fontSize: 11, color: 'var(--muted)' }}>No hay repartidores habilitados</span>
          )}
        </div>
      )}

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {isLoading ? (
          <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Cargando…</p>
        ) : isError ? (
          <p style={{ padding: 32, textAlign: 'center', color: 'var(--error)' }}>No se pudieron cargar los pedidos</p>
        ) : pedidos.length === 0 ? (
          <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Sin pedidos listos para asignar</p>
        ) : (
          <div className="table-scroll">
            <table className="tbl">
              <thead>
                <tr>
                  {puedeGestionar && (
                    <th style={{ width: 36 }}>
                      <input type="checkbox" checked={todosSeleccionados} onChange={toggleTodos} aria-label="Seleccionar todos" />
                    </th>
                  )}
                  {['Pedido', 'Cliente', 'Dirección', 'Teléfono'].map(h => <th key={h}>{h}</th>)}
                  {puedeGestionar && <th>Acciones</th>}
                </tr>
              </thead>
              <tbody>
                {pedidos.map(p => (
                  <tr key={p.pedidoClienteId}>
                    {puedeGestionar && (
                      <td>
                        <input
                          type="checkbox"
                          checked={seleccion.includes(p.pedidoClienteId)}
                          onChange={() => toggle(p.pedidoClienteId)}
                          aria-label={`Seleccionar pedido ${p.codigo ?? p.pedidoClienteId}`}
                        />
                      </td>
                    )}
                    <td style={{ fontWeight: 700, color: 'var(--fuchsia)', fontSize: 12 }}>#{p.codigo ?? p.pedidoClienteId}</td>
                    <td style={{ fontWeight: 600 }}>{p.clienteNombre ?? '—'}</td>
                    <td style={{ fontSize: 12, color: 'var(--muted)', maxWidth: 220 }}>{p.direccion ?? '—'}</td>
                    <td style={{ fontSize: 12, color: 'var(--muted)' }}>{p.telefono ?? '—'}</td>
                    {puedeGestionar && (
                      <td>
                        <button
                          className="btn-ghost"
                          style={{ padding: '6px 12px' }}
                          onClick={() => asignarUno(p.pedidoClienteId)}
                          disabled={sinRepartidor || asignar.isPending}
                          title={sinRepartidor ? 'Elige un repartidor' : 'Asignar'}
                        >
                          Asignar
                        </button>
                      </td>
                    )}
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
