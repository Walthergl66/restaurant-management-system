'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import { listarMesas, crearMesa, actualizarMesa, cambiarHabilitacionMesa } from './api'
import type { Mesa, MesaPayload } from './types'

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
