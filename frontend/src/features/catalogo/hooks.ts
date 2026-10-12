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
  cambiarHabilitacionCatalogo,
  listarAreas, listarExtras,
  crearCategoria, actualizarCategoria, eliminarCategoria,
  crearArea, actualizarArea, eliminarArea,
  crearExtra, actualizarExtra, eliminarExtra,
} from './api'
import type { CatalogoRecurso } from './api'
import type {
  Area, AreaPayload, Categoria, CategoriaPayload, Extra, ExtraPayload,
  Producto, ProductoPayload,
} from './types'

export function useProductos() {
  return useQuery<Producto[]>({
    queryKey: ['productos'],
    queryFn: listarProductos,
  })
}

export function useHabilitacionCatalogo(recurso: CatalogoRecurso) {
  return useMutation({
    mutationFn: ({ id, habilitada }: { id: number; habilitada: boolean }) =>
      cambiarHabilitacionCatalogo(recurso, id, habilitada),
    onSuccess: async (_data, { habilitada }) => {
      toast.success(habilitada ? 'Registro activado' : 'Registro desactivado')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: [recurso] }),
        ...(recurso !== 'productos' ? [queryClient.invalidateQueries({ queryKey: ['productos'] })] : []),
      ])
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo cambiar el estado')),
  })
}

export function useCategorias() {
  return useQuery<Categoria[]>({
    queryKey: ['categorias'],
    queryFn: listarCategorias,
  })
}

export function useAreas() {
  return useQuery<Area[]>({ queryKey: ['areas'], queryFn: listarAreas })
}

export function useExtras() {
  return useQuery<Extra[]>({ queryKey: ['extras'], queryFn: listarExtras })
}

function useGuardarCatalogo<TPayload>(
  key: string,
  crear: (body: TPayload) => Promise<unknown>,
  actualizar: (id: number, body: TPayload) => Promise<unknown>,
  onSaved?: () => void,
) {
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: TPayload }) => id ? actualizar(id, body) : crear(body),
    onSuccess: (_data, variables) => {
      toast.success(variables.id ? 'Registro actualizado' : 'Registro creado')
      queryClient.invalidateQueries({ queryKey: [key] })
      onSaved?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo guardar')),
  })
}

function useDesactivarCatalogo(key: string, eliminar: (id: number) => Promise<void>) {
  return useMutation({
    mutationFn: eliminar,
    onSuccess: () => {
      toast.success('Registro desactivado')
      queryClient.invalidateQueries({ queryKey: [key] })
      if (key !== 'productos') queryClient.invalidateQueries({ queryKey: ['productos'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo desactivar')),
  })
}

export const useGuardarCategoria = (onSaved?: () => void) =>
  useGuardarCatalogo<CategoriaPayload>('categorias', crearCategoria, actualizarCategoria, onSaved)
export const useDesactivarCategoria = () => useDesactivarCatalogo('categorias', eliminarCategoria)
export const useGuardarArea = (onSaved?: () => void) =>
  useGuardarCatalogo<AreaPayload>('areas', crearArea, actualizarArea, onSaved)
export const useDesactivarArea = () => useDesactivarCatalogo('areas', eliminarArea)
export const useGuardarExtra = (onSaved?: () => void) =>
  useGuardarCatalogo<ExtraPayload>('extras', crearExtra, actualizarExtra, onSaved)
export const useDesactivarExtra = () => useDesactivarCatalogo('extras', eliminarExtra)

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
