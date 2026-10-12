'use client'
import api from '@/shared/api/client'
import type { ComandaResponse } from './types'

export async function listarComandas(): Promise<ComandaResponse[]> {
  const { data } = await api.get('/comandas')
  return data as ComandaResponse[]
}

export async function marcarEnPreparacion(id: number): Promise<void> {
  await api.post(`/comandas/${id}/en-preparacion`)
}

export async function marcarListo(id: number): Promise<void> {
  await api.post(`/comandas/${id}/listo`)
}

export async function reimprimirComanda(id: number): Promise<void> {
  await api.post(`/comandas/${id}/reimprimir`)
}
