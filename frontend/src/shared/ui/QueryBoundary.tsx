'use client'
import axios from 'axios'
import type { ReactNode } from 'react'
import { AlertCircle, RefreshCw } from 'lucide-react'

// Contrato mínimo: permite agrupar consultas de distintos tipos sin acoplar la UI a una feature.
interface QueryStatus {
  isPending: boolean
  isError: boolean
  isPaused: boolean
  isFetching: boolean
  error: unknown
  refetch: () => Promise<unknown>
}

export function hasQueryIssue(queries: readonly QueryStatus[]) {
  return queries.some(query => query.isPending || query.isError || query.isPaused)
}

export default function QueryBoundary({ queries, label, children }: {
  queries: readonly QueryStatus[]
  label: string
  children?: ReactNode
}) {
  const failed = queries.filter(query => query.isError)
  const forbidden = failed.some(query => axios.isAxiosError(query.error) && query.error.response?.status === 403)
  const paused = queries.some(query => query.isPaused)
  if (!failed.length && !paused) {
    if (queries.some(query => query.isPending)) {
      return <div className="card" role="status" style={{ padding: 24, color: 'var(--muted)' }}>Cargando {label}…</div>
    }
    return <>{children}</>
  }
  const networkError = failed.some(query => axios.isAxiosError(query.error) && !query.error.response)
  const limited = failed.some(query => axios.isAxiosError(query.error) && query.error.response?.status === 429)
  const title = forbidden ? 'Acceso restringido'
    : paused || networkError ? 'No se pudo conectar'
      : limited ? 'Límite de consultas alcanzado' : `No se pudieron cargar ${label}`
  const message = forbidden ? `Tu usuario no tiene permiso para consultar ${label}.`
    : paused || networkError ? 'Comprueba la conexión e inténtalo nuevamente.'
      : limited ? 'Espera un momento antes de volver a intentar.'
        : 'La información no está disponible en este momento. Inténtalo nuevamente.'
  const retrying = queries.some(query => query.isFetching)
  const retry = () => {
    const affected = queries.filter(query => query.isError || query.isPaused)
    void Promise.allSettled(affected.map(query => query.refetch()))
  }
  return <div className="card" style={{ padding: 24 }}>
    <div role="alert" style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
      <AlertCircle size={20} style={{ flexShrink: 0, color: 'var(--error)' }} />
      <div><p style={{ fontWeight: 700 }}>{title}</p><p style={{ color: 'var(--muted)', fontSize: 13, marginTop: 6 }}>{message}</p></div>
    </div>
    <button className="btn-ghost" onClick={retry} disabled={retrying || paused} style={{ marginTop: 14 }}>
      <RefreshCw size={14} />{retrying ? 'Reintentando…' : paused ? 'Esperando conexión…' : 'Reintentar'}
    </button>
  </div>
}
