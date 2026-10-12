'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import { listarCuentas, cerrarCuenta, crearAdicion, cobrarCuenta } from './api'
import type { AdicionPayload, CobroPayload, CuentaResponse } from './types'

export function useCuentas() {
  return useQuery<CuentaResponse[]>({
    queryKey: ['cuentas'],
    queryFn: listarCuentas,
    refetchInterval: 20_000,
  })
}

export function useCerrarCuenta() {
  return useMutation({
    mutationFn: cerrarCuenta,
    onSuccess: () => {
      toast.success('Cuenta cerrada')
      queryClient.invalidateQueries({ queryKey: ['cuentas'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useCrearAdicion(opts: { onCreated?: () => void } = {}) {
  return useMutation<{ pedidoCodigo: string }, unknown, { id: number; payload: AdicionPayload }>({
    mutationFn: ({ id, payload }) => crearAdicion(id, payload),
    onSuccess: (res) => {
      toast.success(`Adición ${res.pedidoCodigo} creada — agrega ítems al pedido`)
      queryClient.invalidateQueries({ queryKey: ['cuentas'] })
      queryClient.invalidateQueries({ queryKey: ['pedidos'] })
      opts.onCreated?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useCobrar(opts: { onCobrado?: () => void } = {}) {
  return useMutation<void, unknown, { id: number; payload: CobroPayload }>({
    mutationFn: ({ id, payload }) => cobrarCuenta(id, payload),
    onSuccess: () => {
      toast.success('Pago registrado')
      queryClient.invalidateQueries({ queryKey: ['cuentas'] })
      queryClient.invalidateQueries({ queryKey: ['cajas'] })
      opts.onCobrado?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}
