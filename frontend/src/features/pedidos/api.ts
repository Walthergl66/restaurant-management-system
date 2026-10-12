'use client'
import api from '@/shared/api/client'
import type { PedidoResponse, CrearPedidoPayload, PedidoItemPayload } from './types'

export async function listarPedidos(): Promise<PedidoResponse[]> {
  const { data } = await api.get('/pedidos')
  return Array.isArray(data) ? data : data.content ?? []
}

export async function obtenerPedido(codigo: string): Promise<PedidoResponse> {
  const { data } = await api.get(`/pedidos/${codigo}`)
  return data
}

export async function crearPedido(body: CrearPedidoPayload): Promise<PedidoResponse> {
  const { data } = await api.post('/pedidos', body)
  return data
}

export async function agregarItemPedido(codigo: string, item: PedidoItemPayload): Promise<void> {
  await api.post(`/pedidos/${codigo}/items`, item)
}

export async function confirmarPedido(codigo: string, idempotencyKey: string): Promise<void> {
  await api.post(`/pedidos/${codigo}/confirmar`, null, {
    headers: { 'Idempotency-Key': idempotencyKey },
  })
}

export async function anularPedido(codigo: string): Promise<void> {
  await api.delete(`/pedidos/${codigo}`)
}
