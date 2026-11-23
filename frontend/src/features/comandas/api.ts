'use client'
import api from '@/shared/api/client'
import type { ComandaResponse } from './types'

/** Comandas activas: pendientes, en preparación y las listas recientes. */
export async function listarComandas(): Promise<ComandaResponse[]> {
  const { data } = await api.get('/comandas', { params: { activas: true } })
  return data as ComandaResponse[]
}

export async function reimprimirComanda(id: number): Promise<void> {
  await api.post(`/comandas/${id}/reimprimir`)
}
