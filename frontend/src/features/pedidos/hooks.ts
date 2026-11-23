'use client'
import { useQuery } from '@tanstack/react-query'
import { listarPedidos, obtenerPedido } from './api'
import type { PedidoResponse } from './types'

export function usePedidos() {
  return useQuery<PedidoResponse[]>({
    queryKey: ['pedidos', 'list'],
    queryFn: listarPedidos,
    refetchInterval: 15_000,
  })
}

export function usePedido(codigo?: string) {
  return useQuery<PedidoResponse>({
    queryKey: ['pedido', codigo],
    queryFn: () => obtenerPedido(codigo as string),
    enabled: !!codigo,
  })
}
