'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import { listarAnulaciones, aprobarAnulacion, rechazarAnulacion } from './api'
import type { AnulacionResponse, EstadoAnulacion } from './types'

export function useAnulaciones(filtro: EstadoAnulacion | 'TODOS') {
  return useQuery<AnulacionResponse[]>({
    queryKey: ['anulaciones', filtro],
    queryFn: () => listarAnulaciones(filtro),
    refetchInterval: 15_000,
  })
}

export function useAprobarAnulacion() {
  return useMutation({
    mutationFn: aprobarAnulacion,
    onSuccess: () => {
      toast.success('Aprobada')
      queryClient.invalidateQueries({ queryKey: ['anulaciones'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useRechazarAnulacion() {
  return useMutation({
    mutationFn: rechazarAnulacion,
    onSuccess: () => {
      toast.success('Rechazada')
      queryClient.invalidateQueries({ queryKey: ['anulaciones'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}
