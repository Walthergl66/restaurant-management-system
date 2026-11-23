'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { errMsg } from '@/shared/lib/utils'
import { listarComandas, reimprimirComanda } from './api'
import type { ComandaResponse } from './types'

export function useComandas() {
  return useQuery<ComandaResponse[]>({
    queryKey: ['comandas'],
    queryFn: listarComandas,
    refetchInterval: 10_000,
  })
}

export function useReimprimir() {
  return useMutation({
    mutationFn: reimprimirComanda,
    onSuccess: () => toast.success('Reimpresión enviada'),
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}
