'use client'
import { useState } from 'react'
import { CheckCircle2 } from 'lucide-react'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'
import { useAuthStore, hasPermiso } from '@/features/auth'
import { useConfirmacionesPendientes, useResolverConfirmacion } from '../hooks'

export default function ConfirmacionesPendientesPanel() {
  const user = useAuthStore(s => s.user)
  const puedeResolver = hasPermiso(user, 'delivery:confirmacion-resolver')

  const { data: pendientes = [], isLoading, isError } = useConfirmacionesPendientes()
  const resolver = useResolverConfirmacion()

  // Resolución con motivo obligatorio en línea.
  const [resolviendo, setResolviendo] = useState<number | null>(null)
  const [motivo, setMotivo] = useState('')

  if (!puedeResolver) return null

  const confirmarResolver = (paradaId: number) => {
    const m = motivo.trim()
    if (!m) return
    resolver.mutate(
      { paradaId, motivo: m },
      {
        onSuccess: () => {
          setResolviendo(null)
          setMotivo('')
        },
      },
    )
  }

  return (
    <section style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <h2 style={{ fontSize: 16, fontWeight: 800 }}>Pendientes de confirmación del cliente</h2>

      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {isLoading ? (
          <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Cargando…</p>
        ) : isError ? (
          <p style={{ padding: 32, textAlign: 'center', color: 'var(--error)' }}>
            No se pudieron cargar las entregas pendientes
          </p>
        ) : pendientes.length === 0 ? (
          <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>
            Sin entregas pendientes de confirmación
          </p>
        ) : (
          <div className="table-scroll">
            <table className="tbl">
              <thead>
                <tr>{['Pedido', 'Dirección', 'Repartidor confirmó', 'Estado', 'Acciones'].map(h => <th key={h}>{h}</th>)}</tr>
              </thead>
              <tbody>
                {pendientes.map(p => {
                  const editando = resolviendo === p.paradaId
                  return (
                    <tr key={p.paradaId}>
                      <td style={{ fontWeight: 700, color: 'var(--fuchsia)', fontSize: 12 }}>#{p.codigo ?? p.pedidoClienteId}</td>
                      <td style={{ fontSize: 12, color: 'var(--muted)', maxWidth: 220 }}>{p.direccion ?? '—'}</td>
                      <td style={{ fontSize: 11, color: 'var(--muted)' }}>
                        {p.repartidorConfirmoAt
                          ? format(new Date(p.repartidorConfirmoAt), 'dd/MM/yy HH:mm', { locale: es })
                          : '—'}
                      </td>
                      <td>
                        <span className="badge badge-solicitada">
                          {p.faseConfirmacion ?? 'PENDIENTE DE CONFIRMACIÓN DEL CLIENTE'}
                        </span>
                      </td>
                      <td>
                        {!editando && (
                          <button
                            className="btn-ghost"
                            style={{ padding: '6px 12px' }}
                            onClick={() => { setResolviendo(p.paradaId); setMotivo('') }}
                          >
                            Resolver
                          </button>
                        )}
                        {editando && (
                          <div className="inline-control" style={{ flexWrap: 'wrap' }}>
                            <input
                              className="input"
                              style={{ minWidth: 200 }}
                              value={motivo}
                              onChange={e => setMotivo(e.target.value)}
                              placeholder="Motivo (obligatorio)"
                              autoFocus
                            />
                            <button
                              className="btn-fuchsia"
                              style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
                              onClick={() => confirmarResolver(p.paradaId)}
                              disabled={motivo.trim() === '' || resolver.isPending}
                            >
                              <CheckCircle2 size={14} /> Confirmar
                            </button>
                            <button
                              className="btn-ghost"
                              style={{ padding: '6px 10px' }}
                              onClick={() => { setResolviendo(null); setMotivo('') }}
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
