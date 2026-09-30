'use client'
import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import api from '@/lib/api'
import type { UserInfo } from '@/types'

interface AuthState {
  user: UserInfo | null
  isAuthenticated: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
  fetchMe: () => Promise<void>
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      isAuthenticated: false,

      login: async (username, password) => {
        const { data } = await api.post('/auth/login', { username, password })
        localStorage.setItem('access_token', data.accessToken)
        localStorage.setItem('refresh_token', data.refreshToken)
        set({ isAuthenticated: true })
        await get().fetchMe()
      },

      logout: async () => {
        const refreshToken = localStorage.getItem('refresh_token')
        try { if (refreshToken) await api.post('/auth/logout', { refreshToken }) } catch { /* ok */ }
        localStorage.removeItem('access_token')
        localStorage.removeItem('refresh_token')
        set({ user: null, isAuthenticated: false })
      },

      fetchMe: async () => {
        try {
          const { data } = await api.get('/auth/me')
          set({ user: data, isAuthenticated: true })
        } catch {
          set({ user: null, isAuthenticated: false })
        }
      },
    }),
    {
      name: 'auth',
      partialize: (s) => ({ user: s.user, isAuthenticated: s.isAuthenticated }),
    }
  )
)
