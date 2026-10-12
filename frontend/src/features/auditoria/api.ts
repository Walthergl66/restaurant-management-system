'use client'
import api from '@/shared/api/client'
import type { EventoAuditoria } from './types'

export async function listarEventos(params: { desde: string; hasta: string; entidad?: string }): Promise<EventoAuditoria[]> {
  const query: Record<string, string> = {
    desde: `${params.desde}T00:00:00Z`,
    hasta: `${params.hasta}T23:59:59Z`,
  }
  if (params.entidad) query.entidad = params.entidad
  const { data } = await api.get('/auditoria', { params: query })
  return Array.isArray(data) ? data : (data.content ?? [])
}
