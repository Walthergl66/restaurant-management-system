'use client'
import api from '@/shared/api/client'
import type { Producto, Categoria, ProductoPayload } from './types'

export async function listarProductos(): Promise<Producto[]> {
  const { data } = await api.get('/productos')
  return Array.isArray(data) ? data : data.content ?? []
}

export async function listarCategorias(): Promise<Categoria[]> {
  const { data } = await api.get('/categorias')
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

export async function eliminarProducto(id: number): Promise<void> {
  await api.delete(`/productos/${id}`)
}
