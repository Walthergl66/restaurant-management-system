'use client'
import { useMutation, useQuery } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import {
  actualizarDireccionRestaurante,
  asignar,
  asignarLote,
  fijarCoordenadasRestaurante,
  getConfigRestaurante,
  habilitarRepartidor,
  listarAsignaciones,
  listarConfirmacionesPendientes,
  listarPendientesDeAsignar,
  listarRepartidores,
  listarRepartidoresHabilitados,
  reasignar,
  resolverConfirmacion,
  suspenderRepartidor,
} from './api'
import type {
  AsignacionResponse,
  ConfigRestauranteResumen,
  ConfirmacionPendienteResponse,
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

// ---- Ubicación del restaurante ----

export function useConfigRestaurante() {
  return useQuery<ConfigRestauranteResumen>({
    queryKey: ['delivery', 'config-restaurante'],
    queryFn: getConfigRestaurante,
  })
}

export function useActualizarDireccion() {
  return useMutation({
    mutationFn: (direccion: string) => actualizarDireccionRestaurante(direccion),
    onSuccess: (data) => {
      if (data.requiereSeleccionManual) {
        toast('No se pudo ubicar automáticamente, selecciona el punto en el mapa', { icon: '📍' })
      } else {
        toast.success('Dirección actualizada')
      }
      queryClient.setQueryData(['delivery', 'config-restaurante'], data)
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo actualizar la dirección')),
  })
}

export function useFijarCoordenadas() {
  return useMutation({
    mutationFn: (v: { lat: number; lng: number }) => fijarCoordenadasRestaurante(v.lat, v.lng),
    onSuccess: (data) => {
      toast.success('Ubicación guardada')
      queryClient.setQueryData(['delivery', 'config-restaurante'], data)
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo guardar la ubicación')),
  })
}

// ---- Confirmaciones pendientes ----

export function useConfirmacionesPendientes() {
  return useQuery<ConfirmacionPendienteResponse[]>({
    queryKey: ['delivery', 'confirmaciones-pendientes'],
    queryFn: listarConfirmacionesPendientes,
    refetchInterval: REFRESH,
  })
}

export function useResolverConfirmacion() {
  return useMutation({
    mutationFn: (v: { paradaId: number; motivo: string }) =>
      resolverConfirmacion(v.paradaId, v.motivo),
    onSuccess: () => {
      toast.success('Entrega resuelta')
      queryClient.invalidateQueries({ queryKey: ['delivery', 'confirmaciones-pendientes'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo resolver la entrega')),
  })
}
