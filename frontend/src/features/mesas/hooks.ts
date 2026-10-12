'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import { listarMesas, crearMesa, actualizarMesa, cambiarEstadoMesa, eliminarMesa } from './api'
import type { Mesa, MesaPayload, EstadoMesa } from './types'

export function useMesas() {
  return useQuery<Mesa[]>({
    queryKey: ['mesas'],
    queryFn: listarMesas,
    refetchInterval: 20_000,
  })
}

export function useCambiarEstadoMesa() {
  return useMutation({
    mutationFn: ({ id, estado }: { id: number; estado: EstadoMesa }) => cambiarEstadoMesa(id, estado),
    onSuccess: () => {
      toast.success('Estado actualizado')
      queryClient.invalidateQueries({ queryKey: ['mesas'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useEliminarMesa() {
  return useMutation({
    mutationFn: eliminarMesa,
    onSuccess: () => {
      toast.success('Mesa eliminada')
      queryClient.invalidateQueries({ queryKey: ['mesas'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
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
