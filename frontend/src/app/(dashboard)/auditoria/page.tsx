'use client'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import api from '@/lib/api'
import type { EventoAuditoria } from '@/types'
import { format, subDays } from 'date-fns'
import { es } from 'date-fns/locale'

const TIPO_COLOR: Record<string, string> = {
  CREAR: '#22c55e', ACTUALIZAR: '#F5A623', ELIMINAR: '#ef4444',
  LOGIN: '#3b82f6', LOGOUT: '#8b5cf6', APROBAR: '#22c55e', RECHAZAR: '#ef4444',
}

export default function AuditoriaPage() {
  const today = new Date()
  const [desde,   setDesde]   = useState(format(subDays(today, 6), 'yyyy-MM-dd'))
  const [hasta,   setHasta]   = useState(format(today, 'yyyy-MM-dd'))
  const [entidad, setEntidad] = useState('')
  const [search,  setSearch]  = useState('')

  const { data: eventos = [], isLoading } = useQuery<EventoAuditoria[]>({
    queryKey: ['auditoria', desde, hasta, entidad],
    queryFn: async () => {
      const params: Record<string, string> = {
        desde: `${desde}T00:00:00Z`,
        hasta: `${hasta}T23:59:59Z`,
      }
      if (entidad) params.entidad = entidad
      const { data } = await api.get('/auditoria', { params })
      return Array.isArray(data) ? data : data.content ?? []
    },
  })

  const filtered = eventos.filter(e =>
    !search ||
    e.usuario?.toLowerCase().includes(search.toLowerCase()) ||
    e.tipo?.toLowerCase().includes(search.toLowerCase()) ||
    e.entidad?.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <h1 style={{ fontSize: 20, fontWeight: 800 }}>Auditoría</h1>

      {/* filtros */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 12, padding: '8px 14px', flex: 1, minWidth: 200 }}>
          <Search size={15} color="var(--muted)" />
          <input
            className="input"
            style={{ border: 'none', background: 'none', padding: 0, flex: 1 }}
            placeholder="Buscar usuario, tipo, entidad…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
        <input type="date" value={desde} onChange={e => setDesde(e.target.value)} className="input" style={{ width: 148, fontSize: 13 }} />
        <span style={{ alignSelf: 'center', fontSize: 12, color: 'var(--muted)' }}>a</span>
        <input type="date" value={hasta} onChange={e => setHasta(e.target.value)} className="input" style={{ width: 148, fontSize: 13 }} />
        <input placeholder="Filtrar entidad…" value={entidad} onChange={e => setEntidad(e.target.value)} className="input" style={{ width: 160, fontSize: 13 }} />
      </div>

      {/* stat */}
      <div style={{ display: 'flex', gap: 8 }}>
        <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 10, padding: '6px 14px', fontSize: 13 }}>
          <span style={{ color: 'var(--muted)' }}>Total eventos: </span>
          <strong style={{ color: 'var(--yellow)' }}>{filtered.length}</strong>
        </div>
      </div>

      {/* tabla */}
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        {isLoading
          ? <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Cargando…</p>
          : filtered.length === 0
            ? <p style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Sin eventos para el período</p>
            : (
              <div style={{ overflowX: 'auto' }}>
                <table className="tbl">
                  <thead>
                    <tr>{['Fecha', 'Usuario', 'Tipo', 'Entidad', 'ID', 'Detalle'].map(h => <th key={h}>{h}</th>)}</tr>
                  </thead>
                  <tbody>
                    {filtered.slice(0, 200).map(ev => (
                      <tr key={ev.id}>
                        <td style={{ fontSize: 11, color: 'var(--muted)', whiteSpace: 'nowrap' }}>
                          {ev.fecha ? format(new Date(ev.fecha), 'dd/MM/yy HH:mm:ss', { locale: es }) : '—'}
                        </td>
                        <td style={{ fontWeight: 600, color: 'var(--yellow)', fontSize: 12 }}>{ev.usuario}</td>
                        <td>
                          <span style={{
                            fontSize: 11, fontWeight: 700, borderRadius: 20, padding: '2px 8px',
                            background: `${TIPO_COLOR[ev.tipo] ?? '#888'}22`,
                            color: TIPO_COLOR[ev.tipo] ?? 'var(--muted)',
                          }}>{ev.tipo}</span>
                        </td>
                        <td style={{ fontSize: 12 }}>{ev.entidad}</td>
                        <td style={{ fontSize: 11, color: 'var(--muted)' }}>{ev.entidadId ?? '—'}</td>
                        <td style={{ fontSize: 11, color: 'var(--muted)', maxWidth: 200 }}>
                          {ev.detalle
                            ? <span title={ev.detalle}>{ev.detalle.length > 60 ? ev.detalle.slice(0, 60) + '…' : ev.detalle}</span>
                            : '—'
                          }
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )
        }
      </div>
    </div>
  )
}
