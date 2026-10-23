'use client'
import { useMutation, useQuery } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import {
  asignar,
  asignarLote,
  habilitarRepartidor,
  listarAsignaciones,
  listarPendientesDeAsignar,
  listarRepartidores,
  listarRepartidoresHabilitados,
  reasignar,
  suspenderRepartidor,
} from './api'
import type {
  AsignacionResponse,
  EstadoAsignacion,
  PedidoListoResponse,
  RepartidorResponse,
} from './types'

const REFRESH = 20_000

function invalidarTodo() {
  queryClient.invalidateQueries({ queryKey: ['delivery'] })
}

// ---- Queries ----

export function useRepartidores() {
  return useQuery<RepartidorResponse[]>({
    queryKey: ['delivery', 'repartidores'],
    queryFn: listarRepartidores,
    refetchInterval: REFRESH,
  })
}

export function useRepartidoresHabilitados() {
  return useQuery<RepartidorResponse[]>({
    queryKey: ['delivery', 'repartidores', 'habilitados'],
    queryFn: listarRepartidoresHabilitados,
    refetchInterval: REFRESH,
  })
}

export function usePedidosPendientes() {
  return useQuery<PedidoListoResponse[]>({
    queryKey: ['delivery', 'pendientes-de-asignar'],
    queryFn: listarPendientesDeAsignar,
    refetchInterval: REFRESH,
  })
}

export function useAsignaciones(filtro: { estado?: EstadoAsignacion; repartidorId?: number } = {}) {
  return useQuery<AsignacionResponse[]>({
    queryKey: ['delivery', 'asignaciones', filtro],
    queryFn: () => listarAsignaciones(filtro),
    refetchInterval: REFRESH,
  })
}

// ---- Mutations ----

export function useHabilitarRepartidor() {
  return useMutation({
    mutationFn: habilitarRepartidor,
    onSuccess: () => {
      toast.success('Repartidor habilitado')
      invalidarTodo()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo habilitar')),
  })
}

export function useSuspenderRepartidor() {
  return useMutation({
    mutationFn: suspenderRepartidor,
    onSuccess: () => {
      toast.success('Repartidor suspendido')
      invalidarTodo()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo suspender')),
  })
}

export function useAsignar() {
  return useMutation({
    mutationFn: (v: { pedidoClienteId: number; repartidorId: number }) =>
      asignar(v.pedidoClienteId, v.repartidorId),
    onSuccess: () => {
      toast.success('Pedido asignado')
      invalidarTodo()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo asignar')),
  })
}

export function useAsignarLote() {
  return useMutation({
    mutationFn: (v: { pedidoClienteIds: number[]; repartidorId: number }) =>
      asignarLote(v.pedidoClienteIds, v.repartidorId),
    onSuccess: (data) => {
      toast.success(`${data.length} pedido${data.length > 1 ? 's' : ''} asignado${data.length > 1 ? 's' : ''}`)
      invalidarTodo()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo asignar el lote')),
  })
}

export function useReasignar() {
  return useMutation({
    mutationFn: (v: { pedidoClienteId: number; repartidorId: number }) =>
      reasignar(v.pedidoClienteId, v.repartidorId),
    onSuccess: () => {
      toast.success('Pedido reasignado')
      invalidarTodo()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo reasignar')),
  })
}
