'use client'
import api from '@/shared/api/client'
import type { AuthResponse, UserInfo } from './types'
import { readSession, revokeSession } from '@/shared/api/session'

export async function login(username: string, password: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', { username, password }, { timeout: 15_000 })
  return data
}

export async function logout(refreshToken: string): Promise<void> {
  // Compatibilidad con la superficie pública del módulo; la salida normal se centraliza en el store.
  const accessToken = readSession()?.accessToken
  if (accessToken) await revokeSession({ accessToken, refreshToken })
}

export async function me(): Promise<UserInfo> {
  const { data } = await api.get<UserInfo>('/auth/me', { timeout: 15_000 })
  return data
}
