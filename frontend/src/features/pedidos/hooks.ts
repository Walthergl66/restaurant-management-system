'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import { listarMesas } from '@/features/mesas/api'
import { listarProductos } from '@/features/catalogo/api'
import type { Mesa } from '@/features/mesas/types'
import type { Producto } from '@/features/catalogo/types'
import {
  listarPedidos,
  obtenerPedido,
  crearPedido,
  agregarItemPedido,
  confirmarPedido,
  anularPedido,
} from './api'
import { nuevoCodigoPedido } from './lib/codigo'
import type { PedidoResponse, CrearPedidoInput } from './types'

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

export function useMesasLibres() {
  return useQuery<Mesa[]>({
    queryKey: ['mesas', 'libres'],
    queryFn: async () => (await listarMesas()).filter(m => m.activo && m.estado === 'LIBRE'),
  })
}

export function useProductosActivos() {
  return useQuery<Producto[]>({
    queryKey: ['productos', 'activos'],
    queryFn: async () => (await listarProductos()).filter(p => p.activo),
  })
}

export function useConfirmarPedido(opts: { mensaje?: string; onConfirmed?: () => void } = {}) {
  const { mensaje = 'Pedido confirmado', onConfirmed } = opts
  return useMutation({
    mutationFn: (codigo: string) => confirmarPedido(codigo, `web-${codigo}-${Date.now()}`),
    onSuccess: () => {
      toast.success(mensaje)
      queryClient.invalidateQueries({ queryKey: ['pedidos'] })
      onConfirmed?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error al confirmar')),
  })
}

export function useAnularPedido(opts: { mensaje?: string; onAnulado?: () => void; invalidar?: boolean } = {}) {
  const { mensaje = 'Pedido anulado', onAnulado, invalidar = true } = opts
  return useMutation({
    mutationFn: (codigo: string) => anularPedido(codigo),
    onSuccess: () => {
      toast.success(mensaje)
      if (invalidar) queryClient.invalidateQueries({ queryKey: ['pedidos'] })
      onAnulado?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error al anular')),
  })
}

export function useCrearPedido(onCreated?: (codigo: string) => void) {
  return useMutation({
    mutationFn: async ({ mesaId, notas, confirmar, items }: CrearPedidoInput) => {
      const codigo = nuevoCodigoPedido()
      await crearPedido({ codigo, mesaId, notas })
      for (const item of items) {
        await agregarItemPedido(codigo, item)
      }
      if (confirmar) {
        await confirmarPedido(codigo, codigo)
      }
      return codigo
    },
    onSuccess: (codigo, variables) => {
      toast.success(`Pedido ${codigo} ${variables.confirmar ? 'confirmado' : 'creado'}`)
      queryClient.invalidateQueries({ queryKey: ['pedidos'] })
      queryClient.invalidateQueries({ queryKey: ['mesas'] })
      queryClient.invalidateQueries({ queryKey: ['cuentas'] })
      onCreated?.(codigo)
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error al crear pedido')),
  })
}
