'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import {
  listarProductos,
  listarCategorias,
  crearProducto,
  actualizarProducto,
  eliminarProducto,
} from './api'
import type { Producto, Categoria, ProductoPayload } from './types'

export function useProductos() {
  return useQuery<Producto[]>({
    queryKey: ['productos'],
    queryFn: listarProductos,
  })
}

export function useCategorias() {
  return useQuery<Categoria[]>({
    queryKey: ['categorias'],
    queryFn: listarCategorias,
  })
}

export function useEliminarProducto() {
  return useMutation({
    mutationFn: eliminarProducto,
    onSuccess: () => {
      toast.success('Eliminado')
      queryClient.invalidateQueries({ queryKey: ['productos'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useGuardarProducto(onSaved?: () => void) {
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: ProductoPayload }) =>
      id ? actualizarProducto(id, body) : crearProducto(body),
    onSuccess: (_data, variables) => {
      toast.success(variables.id ? 'Actualizado' : 'Creado')
      queryClient.invalidateQueries({ queryKey: ['productos'] })
      onSaved?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}
