'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import {
  listarMesas, crearMesa, actualizarMesa, cambiarHabilitacionMesa,
  listarZonas, crearZona, renombrarZona, ordenarZonas, eliminarZona,
} from './api'
import type { Mesa, MesaPayload, Zona } from './types'

export function useMesas() {
  return useQuery<Mesa[]>({
    queryKey: ['mesas'],
    queryFn: listarMesas,
    refetchInterval: 20_000,
  })
}

export function useCambiarHabilitacionMesa() {
  return useMutation({
    mutationFn: ({ id, habilitada }: { id: number; habilitada: boolean }) => cambiarHabilitacionMesa(id, habilitada),
    onSuccess: () => {
      toast.success('Disponibilidad actualizada')
      queryClient.invalidateQueries({ queryKey: ['mesas'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo actualizar la disponibilidad')),
  })
}

export function useGuardarMesa(onSaved?: () => void) {
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: MesaPayload }) =>
      id ? actualizarMesa(id, body) : crearMesa(body),
    onSuccess: (_data, variables) => {
      toast.success(variables.id ? 'Mesa actualizada' : 'Mesa creada')
      queryClient.invalidateQueries({ queryKey: ['mesas'] })
      onSaved?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useZonas() {
  return useQuery<Zona[]>({
    queryKey: ['zonas'],
    queryFn: listarZonas,
  })
}

/** Las zonas y las mesas se muestran juntas: cualquier cambio en una refresca ambas. */
function refrescarZonasYMesas() {
  queryClient.invalidateQueries({ queryKey: ['zonas'] })
  queryClient.invalidateQueries({ queryKey: ['mesas'] })
}

export function useGuardarZona(onSaved?: (zona: Zona) => void) {
  return useMutation({
    mutationFn: ({ id, nombre }: { id?: number; nombre: string }) =>
      id ? renombrarZona(id, nombre) : crearZona(nombre),
    onSuccess: (zona, variables) => {
      toast.success(variables.id ? 'Zona actualizada' : 'Zona creada')
      refrescarZonasYMesas()
      onSaved?.(zona)
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo guardar la zona')),
  })
}

export function useOrdenarZonas() {
  return useMutation({
    mutationFn: (ids: number[]) => ordenarZonas(ids),
    onSuccess: refrescarZonasYMesas,
    onError: (e: unknown) => {
      toast.error(errMsg(e, 'No se pudo guardar el orden'))
      refrescarZonasYMesas()
    },
  })
}

export function useEliminarZona() {
  return useMutation({
    mutationFn: (id: number) => eliminarZona(id),
    onSuccess: () => {
      toast.success('Zona eliminada')
      refrescarZonasYMesas()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo eliminar la zona')),
  })
}
