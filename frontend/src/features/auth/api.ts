'use client'
import api from '@/shared/api/client'
import type { AuthResponse, UserInfo } from './types'

export async function login(username: string, password: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', { username, password })
  return data
}

export async function refresh(refreshToken: string): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/refresh', { refreshToken })
  return data
}

export async function logout(refreshToken: string): Promise<void> {
  await api.post('/auth/logout', { refreshToken })
}

export async function me(): Promise<UserInfo> {
  const { data } = await api.get<UserInfo>('/auth/me')
  return data
}
