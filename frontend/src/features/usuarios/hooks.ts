'use client'
import { useQuery, useMutation } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { queryClient } from '@/shared/api/queryClient'
import { errMsg } from '@/shared/lib/utils'
import { listarUsuarios, listarRoles, listarPermisos, eliminarUsuario, cambiarEstadoUsuario, crearUsuario, actualizarUsuario } from './api'
import type { ActualizarUsuarioPayload, CrearUsuarioPayload, PermisoDto, RolDto, UsuarioAdminResponse } from './types'

export function useUsuarios() {
  return useQuery<UsuarioAdminResponse[]>({
    queryKey: ['usuarios'],
    queryFn: listarUsuarios,
  })
}

export function useRoles() {
  return useQuery<RolDto[]>({
    queryKey: ['roles'],
    queryFn: listarRoles,
  })
}

export function usePermisos() {
  return useQuery<PermisoDto[]>({
    queryKey: ['permisos'],
    queryFn: listarPermisos,
  })
}

export function useEliminarUsuario() {
  return useMutation({
    mutationFn: eliminarUsuario,
    onSuccess: () => {
      toast.success('Usuario eliminado')
      queryClient.invalidateQueries({ queryKey: ['usuarios'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useCambiarEstadoUsuario() {
  return useMutation<void, unknown, { id: number; activo: boolean }>({
    mutationFn: ({ id, activo }) => cambiarEstadoUsuario(id, activo),
    onSuccess: (_data, { activo }) => {
      toast.success(activo ? 'Usuario activado' : 'Usuario desactivado')
      queryClient.invalidateQueries({ queryKey: ['usuarios'] })
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useCrearUsuario(opts: { onSaved?: () => void } = {}) {
  return useMutation<void, unknown, CrearUsuarioPayload>({
    mutationFn: crearUsuario,
    onSuccess: () => {
      toast.success('Usuario creado')
      queryClient.invalidateQueries({ queryKey: ['usuarios'] })
      opts.onSaved?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}

export function useActualizarUsuario(opts: { onSaved?: () => void } = {}) {
  return useMutation<void, unknown, { id: number; payload: ActualizarUsuarioPayload }>({
    mutationFn: ({ id, payload }) => actualizarUsuario(id, payload),
    onSuccess: () => {
      toast.success('Usuario actualizado')
      queryClient.invalidateQueries({ queryKey: ['usuarios'] })
      opts.onSaved?.()
    },
    onError: (e: unknown) => toast.error(errMsg(e, 'Error')),
  })
}
