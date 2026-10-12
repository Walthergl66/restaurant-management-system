'use client'
import { useQuery } from '@tanstack/react-query'
import { listarEventos } from './api'
import type { EventoAuditoria } from './types'

export function useAuditoria(params: { desde: string; hasta: string; entidad?: string }) {
  return useQuery<EventoAuditoria[]>({
    queryKey: ['auditoria', params.desde, params.hasta, params.entidad ?? ''],
    queryFn: () => listarEventos(params),
  })
}
