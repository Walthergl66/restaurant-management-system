'use client'
import axios from 'axios'
import { create } from 'zustand'
import { login as loginApi, me as meApi } from './api'
import { queryClient } from '@/shared/api/queryClient'
import { endSession, readSession, revokeSession, SessionUnavailableError, startSession } from '@/shared/api/session'
import type { UserInfo } from './types'

export { hasPermiso } from '@/shared/lib/permissions'

type SessionStatus = 'unknown' | 'checking' | 'authenticated' | 'anonymous' | 'error'
interface AuthState {
  user: UserInfo | null
  isAuthenticated: boolean
  status: SessionStatus
  sessionId: string | null
  sessionError: string | null
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
  fetchMe: () => Promise<void>
  initializeSession: () => Promise<void>
  syncSession: () => Promise<void>
  sessionUnavailable: (id: string, message: string) => void
}

let verification: { id: string; promise: Promise<void> } | null = null

function clearPrivateData() {
  void queryClient.cancelQueries()
  queryClient.clear()
}

export const useAuthStore = create<AuthState>()((set, get) => ({
  user: null,
  isAuthenticated: false,
  status: 'unknown',
  sessionId: null,
  sessionError: null,

  sessionUnavailable: (id, message) => {
    if (readSession()?.id === id && get().sessionId === id) {
      set({ status: 'error', isAuthenticated: false, sessionError: message })
    }
  },

  login: async (username, password) => {
    const previousId = readSession()?.id ?? null
    const data = await loginApi(username, password)
    try {
      if ((readSession()?.id ?? null) !== previousId) throw new axios.CanceledError('La sesión cambió')
      const session = await startSession(data, previousId)
      clearPrivateData()
      // El login ya devuelve la identidad validada por el servidor.
      set({ user: data.usuario, isAuthenticated: true, status: 'authenticated', sessionId: session.id, sessionError: null })
    } catch (error) {
      void revokeSession(data).catch(() => {})
      throw error
    }
  },

  logout: async () => {
    const session = readSession()
    // La salida local es inmediata, incluso sin conexión o con un refresh en curso.
    endSession(session?.id ?? null)
    clearPrivateData()
    set({ user: null, isAuthenticated: false, status: 'anonymous', sessionId: readSession()?.id ?? null, sessionError: null })
    try {
      if (session?.accessToken && session.refreshToken) await revokeSession({ accessToken: session.accessToken, refreshToken: session.refreshToken })
    } catch { /* La sesión local ya terminó; una respuesta tardía no puede restaurarla. */ }
  },

  fetchMe: () => {
    const session = readSession()
    if (!session?.accessToken && !session?.refreshToken) {
      clearPrivateData()
      set({ user: null, isAuthenticated: false, status: 'anonymous', sessionId: session?.id ?? null, sessionError: null })
      return Promise.resolve()
    }
    if (verification?.id === session.id) return verification.promise
    set({ status: 'checking', isAuthenticated: false, sessionId: session.id, sessionError: null })
    const promise = Promise.resolve().then(async () => {
      try {
        const user = await meApi()
        if (readSession()?.id !== session.id) return
        set({ user, isAuthenticated: true, status: 'authenticated', sessionError: null })
      } catch (error) {
        if (readSession()?.id !== session.id) return
        const status = axios.isAxiosError(error) ? error.response?.status : undefined
        const message = error instanceof SessionUnavailableError ? error.message
          : status === 429 ? 'Se alcanzó el límite de intentos. Espera un momento antes de reintentar.'
            : status === 403 ? 'No se pudo verificar el acceso de tu usuario.'
              : 'No se pudo comprobar la sesión. Revisa la conexión e intenta nuevamente.'
        set({ status: 'error', isAuthenticated: false, sessionError: message })
      } finally {
        if (verification?.promise === promise) verification = null
      }
    })
    verification = { id: session.id, promise }
    return promise
  },

  initializeSession: async () => {
    if (get().status === 'unknown') await get().fetchMe()
    else await get().syncSession()
  },

  syncSession: async () => {
    const session = readSession()
    if ((session?.id ?? null) === get().sessionId) return // Una rotación de tokens no cambia de usuario.
    clearPrivateData()
    set({ user: null, isAuthenticated: false, status: 'unknown', sessionId: session?.id ?? null, sessionError: null })
    await get().fetchMe()
  },
}))
