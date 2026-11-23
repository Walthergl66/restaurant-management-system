'use client'
import api from '@/shared/api/client'
import type { PedidoResponse } from './types'

export async function listarPedidos(): Promise<PedidoResponse[]> {
  const { data } = await api.get('/pedidos')
  return Array.isArray(data) ? data : data.content ?? []
}

export async function obtenerPedido(codigo: string): Promise<PedidoResponse> {
  const { data } = await api.get(`/pedidos/${codigo}`)
  return data
}
