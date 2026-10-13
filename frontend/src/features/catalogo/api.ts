'use client'
import api from '@/shared/api/client'
import type {
  Area, AreaPayload, Categoria, CategoriaPayload, Extra, ExtraPayload,
  Producto, ProductoPayload, CatalogoRecurso, OrdenCategoriasPayload,
} from './types'

export async function cambiarHabilitacionCatalogo(recurso: CatalogoRecurso, id: number, habilitada: boolean): Promise<void> {
  await api.patch(`/${recurso}/${id}/habilitacion`, { habilitada })
}

export async function listarProductos(): Promise<Producto[]> {
  const { data } = await api.get('/productos')
  return Array.isArray(data) ? data : data.content ?? []
}

export async function listarCategorias(): Promise<Categoria[]> {
  const { data } = await api.get('/categorias')
  return Array.isArray(data) ? data : data.content ?? []
}

export async function guardarOrdenCategorias(body: OrdenCategoriasPayload): Promise<Categoria[]> {
  return (await api.put('/categorias/orden', body)).data
}

export async function listarAreas(): Promise<Area[]> {
  const { data } = await api.get('/areas')
  return Array.isArray(data) ? data : data.content ?? []
}

export async function listarExtras(): Promise<Extra[]> {
  const { data } = await api.get('/extras')
  return Array.isArray(data) ? data : data.content ?? []
}

export async function crearProducto(body: ProductoPayload): Promise<Producto> {
  const { data } = await api.post('/productos', body)
  return data
}

export async function actualizarProducto(id: number, body: ProductoPayload): Promise<Producto> {
  const { data } = await api.put(`/productos/${id}`, body)
  return data
}

export const crearCategoria = async (body: CategoriaPayload): Promise<Categoria> =>
  (await api.post('/categorias', body)).data
export const actualizarCategoria = async (id: number, body: CategoriaPayload): Promise<Categoria> =>
  (await api.put(`/categorias/${id}`, body)).data

export const crearArea = async (body: AreaPayload): Promise<Area> =>
  (await api.post('/areas', body)).data
export const actualizarArea = async (id: number, body: AreaPayload): Promise<Area> =>
  (await api.put(`/areas/${id}`, body)).data

export const crearExtra = async (body: ExtraPayload): Promise<Extra> =>
  (await api.post('/extras', body)).data
export const actualizarExtra = async (id: number, body: ExtraPayload): Promise<Extra> =>
  (await api.put(`/extras/${id}`, body)).data
