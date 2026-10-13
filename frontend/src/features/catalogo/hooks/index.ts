'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import {
  listarProductos, listarCategorias, listarAreas, listarExtras,
  guardarOrdenCategorias, cambiarHabilitacionCatalogo,
  crearProducto, actualizarProducto, crearCategoria, actualizarCategoria,
  crearArea, actualizarArea, crearExtra, actualizarExtra,
} from '../api'
import type {
  Area, AreaPayload, Categoria, CategoriaPayload, Extra, ExtraPayload,
  Producto, ProductoPayload, CatalogoRecurso,
} from '../types'

/** Los nombres y relaciones del catálogo también se muestran en las tarjetas de productos. */
async function invalidarCatalogo(recurso: CatalogoRecurso) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: [recurso] }),
    ...(recurso !== 'productos' ? [queryClient.invalidateQueries({ queryKey: ['productos'] })] : []),
  ])
}

export function useProductos() {
  return useQuery<Producto[]>({ queryKey: ['productos'], queryFn: listarProductos })
}

export function useCategorias() {
  return useQuery<Categoria[]>({ queryKey: ['categorias'], queryFn: listarCategorias })
}

export function useAreas() {
  return useQuery<Area[]>({ queryKey: ['areas'], queryFn: listarAreas })
}

export function useExtras() {
  return useQuery<Extra[]>({ queryKey: ['extras'], queryFn: listarExtras })
}

export function useHabilitacionCatalogo(recurso: CatalogoRecurso) {
  return useMutation({
    mutationFn: ({ id, habilitada }: { id: number; habilitada: boolean }) =>
      cambiarHabilitacionCatalogo(recurso, id, habilitada),
    onSuccess: async (_data, { habilitada }) => {
      toast.success(habilitada ? 'Registro activado' : 'Registro desactivado')
      await invalidarCatalogo(recurso)
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo cambiar el estado')),
  })
}

export function useOrdenCategorias() {
  return useMutation({
    mutationFn: guardarOrdenCategorias,
    onSuccess: async (categorias) => {
      queryClient.setQueryData(['categorias'], categorias)
      toast.success('Orden del menú actualizado')
      await invalidarCatalogo('categorias')
    },
    onError: async (e: unknown) => {
      toast.error(errMsg(e, 'No se pudo guardar el orden'))
      // Recupera la lista actual si otro administrador agregó categorías durante el arrastre.
      await queryClient.invalidateQueries({ queryKey: ['categorias'] })
    },
  })
}

function useGuardarCatalogo<TPayload>(
  recurso: CatalogoRecurso,
  crear: (body: TPayload) => Promise<unknown>,
  actualizar: (id: number, body: TPayload) => Promise<unknown>,
  onSaved?: () => void,
) {
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: TPayload }) =>
      id !== undefined ? actualizar(id, body) : crear(body),
    onSuccess: async (_data, variables) => {
      toast.success(variables.id !== undefined ? 'Registro actualizado' : 'Registro creado')
      await invalidarCatalogo(recurso)
      onSaved?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'No se pudo guardar')),
  })
}

export const useGuardarProducto = (onSaved?: () => void) =>
  useGuardarCatalogo<ProductoPayload>('productos', crearProducto, actualizarProducto, onSaved)
export const useGuardarCategoria = (onSaved?: () => void) =>
  useGuardarCatalogo<CategoriaPayload>('categorias', crearCategoria, actualizarCategoria, onSaved)
export const useGuardarArea = (onSaved?: () => void) =>
  useGuardarCatalogo<AreaPayload>('areas', crearArea, actualizarArea, onSaved)
export const useGuardarExtra = (onSaved?: () => void) =>
  useGuardarCatalogo<ExtraPayload>('extras', crearExtra, actualizarExtra, onSaved)
