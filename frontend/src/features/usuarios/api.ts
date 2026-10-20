'use client'
import api from '@/shared/api/client'
import type { ActualizarUsuarioPayload, CrearUsuarioPayload, PermisoDto, RolDto, UsuarioAdminResponse } from './types'

export async function listarUsuarios(): Promise<UsuarioAdminResponse[]> {
  const { data } = await api.get('/usuarios', { params: { pagina: 0, tamanio: 100 } })
  return data.content ?? data
}

export async function listarRoles(): Promise<RolDto[]> {
  const { data } = await api.get('/usuarios/roles')
  return data
}

export async function listarPermisos(): Promise<PermisoDto[]> {
  const { data } = await api.get('/usuarios/permisos')
  return data
}

export async function eliminarUsuario(id: number): Promise<void> {
  await api.delete(`/usuarios/${id}`)
}

export async function cambiarEstadoUsuario(id: number, activo: boolean): Promise<void> {
  await api.patch(`/usuarios/${id}/estado`, { activo })
}

export async function crearUsuario(payload: CrearUsuarioPayload): Promise<void> {
  await api.post('/usuarios', payload)
}

export async function actualizarUsuario(id: number, payload: ActualizarUsuarioPayload): Promise<void> {
  await api.put(`/usuarios/${id}`, payload)
}
