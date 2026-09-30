'use client'
import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { Check, X } from 'lucide-react'
import api from '@/lib/api'
import { queryClient } from '@/lib/queryClient'
import type { AnulacionResponse, EstadoAnulacion } from '@/types'
import toast from 'react-hot-toast'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'

const CFG: Record<EstadoAnulacion, { label: string; badge: string }> = {
  SOLICITADA: { label: 'Solicitada', badge: 'badge-solicitada' },
  APROBADA:   { label: 'Aprobada',   badge: 'badge-aprobada'   },
  RECHAZADA:  { label: 'Rechazada',  badge: 'badge-rechazada'  },
}

export default function AnulacionesPage() {
  const [filtro, setFiltro] = useState<EstadoAnulacion | 'TODOS'>('TODOS')

  const { data: anulaciones = [], isLoading } = useQuery<AnulacionResponse[]>({
    queryKey: ['anulaciones', filtro],
    queryFn: async () => {
      const params: Record<string, string> = {}
      if (filtro !== 'TODOS') params.estado = filtro
      const { data } = await api.get('/anulaciones', { params })
      return data
    },
    refetchInterval: 15_000,
  })

  const aprobar = useMutation({
    mutationFn: (id: number) => api.patch(`/anulaciones/${id}/aprobar`),
    onSuccess: () => { toast.success('Aprobada'); queryClient.invalidateQueries({ queryKey: ['anulaciones'] }) },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  const rechazar = useMutation({
    mutationFn: (id: number) => api.patch(`/anulaciones/${id}/rechazar`),
    onSuccess: () => { toast.success('Rechazada'); queryClient.invalidateQueries({ queryKey: ['anulaciones'] }) },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  const pendientes = anulaciones.filter(a => a.estado === 'SOLICITADA').length

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
        <h1 style={{ fontSize: 20, fontWeight: 800 }}>Anulaciones</h1>
        {pendientes > 0 && (
          <span style={{ background: 'var(--fuchsia)', color: '#fff', fontSize: 12, fontWeight: 700, borderRadius: 20, padding: '2px 10px' }}>
            {pendientes} pendiente{pendientes > 1 ? 's' : ''}
          </span>
        )}
      </div>

      {/* filtros */}
      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        {(['TODOS', 'SOLICITADA', 'APROBADA', 'RECHAZADA'] as const).map(e => (
          <button key={e} onClick={() => setFiltro(e)} style={{
            padding: '8px 14px', borderRadius: 10, fontSize: 12, fontWeight: 600, cursor: 'pointer', border: 'none',
            background: filtro === e ? 'var(--yellow)' : 'var(--surface)',
            color: filtro === e ? '#000' : 'var(--muted)',
            outline: filtro !== e ? '1px solid var(--border)' : 'none',
          }}>
            {e === 'TODOS' ? 'Todas' : CFG[e].label}
          </button>
        ))}
      </div>

      {/* tabla */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {isLoading
          ? <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Cargando…</p>
          : anulaciones.length === 0
            ? <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Sin anulaciones</p>
            : (
              <table className="tbl">
                <thead>
                  <tr>{['Pedido', 'Producto', 'Cant.', 'Precio', 'Motivo', 'Estado', 'Solicitado por', 'Fecha', 'Acciones'].map(h => <th key={h}>{h}</th>)}</tr>
                </thead>
                <tbody>
                  {anulaciones.map(a => (
                    <tr key={a.id}>
                      <td style={{ fontWeight: 700, color: 'var(--fuchsia)', fontSize: 12 }}>#{a.pedidoCodigo}</td>
                      <td style={{ fontWeight: 600 }}>{a.nombreProducto}</td>
                      <td style={{ color: 'var(--muted)' }}>{a.cantidad}</td>
                      <td style={{ color: 'var(--yellow)', fontWeight: 700 }}>${a.precioUnitario?.toFixed(2)}</td>
                      <td style={{ fontSize: 12, color: 'var(--muted)', maxWidth: 130 }}>
                        <span style={{ overflow: 'hidden', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>{a.motivo ?? '—'}</span>
                      </td>
                      <td><span className={`badge ${CFG[a.estado]?.badge}`}>{CFG[a.estado]?.label}</span></td>
                      <td style={{ fontSize: 12, color: 'var(--muted)' }}>{a.solicitadoPor}</td>
                      <td style={{ fontSize: 11, color: 'var(--muted)' }}>{a.creadoAt ? format(new Date(a.creadoAt), 'dd/MM/yy HH:mm', { locale: es }) : '—'}</td>
                      <td>
                        {a.estado === 'SOLICITADA' && (
                          <div style={{ display: 'flex', gap: 6 }}>
                            <button
                              onClick={() => aprobar.mutate(a.id)}
                              style={{ width: 28, height: 28, borderRadius: 8, background: 'rgba(34,197,94,.15)', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--success)' }}
                              title="Aprobar"
                            ><Check size={13} /></button>
                            <button
                              onClick={() => rechazar.mutate(a.id)}
                              style={{ width: 28, height: 28, borderRadius: 8, background: 'rgba(239,68,68,.15)', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--error)' }}
                              title="Rechazar"
                            ><X size={13} /></button>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )
        }
      </div>
    </div>
  )
}
