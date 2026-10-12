'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import { listarComandas, marcarEnPreparacion, marcarListo, reimprimirComanda } from './api'
import type { ComandaResponse } from './types'

export function useComandas() {
  return useQuery<ComandaResponse[]>({
    queryKey: ['comandas'],
    queryFn: async () => (await listarComandas()).filter(c => c.tipo === 'ORDEN'),
    refetchInterval: 10_000,
  })
}

export function useEnPreparacion() {
  return useMutation({
    mutationFn: marcarEnPreparacion,
    onSuccess: () => {
      toast.success('En preparación')
      queryClient.invalidateQueries({ queryKey: ['comandas'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useListo() {
  return useMutation({
    mutationFn: marcarListo,
    onSuccess: () => {
      toast.success('¡Lista!')
      queryClient.invalidateQueries({ queryKey: ['comandas'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useReimprimir() {
  return useMutation({
    mutationFn: reimprimirComanda,
    onSuccess: () => toast.success('Reimpresión enviada'),
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}
