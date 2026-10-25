'use client'
import axios, { type InternalAxiosRequestConfig } from 'axios'
import { endSession, readSession, refreshSession } from './session'

type SessionRequest = InternalAxiosRequestConfig & { _retry?: boolean; _sessionId?: string | null; _accessToken?: string | null }

function isSessionEndpoint(url = '') {
  return url.startsWith('/auth/') && url !== '/auth/me'
}

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config: SessionRequest) => {
  if ((!config.method || config.method.toLowerCase() === 'get') && !config.timeout) config.timeout = 15_000
  if (typeof window !== 'undefined' && !isSessionEndpoint(config.url)) {
    const session = readSession()
    if (config._retry && config._sessionId !== (session?.id ?? null)) throw new axios.CanceledError('La sesión cambió')
    config._sessionId = session?.id ?? null
    config._accessToken = session?.accessToken ?? null
    if (session?.accessToken) config.headers.set('Authorization', `Bearer ${session.accessToken}`)
    else config.headers.delete('Authorization')
  }
  return config
})

api.interceptors.response.use(
  (res) => {
    const config = res.config as SessionRequest
    if (typeof window !== 'undefined' && !isSessionEndpoint(config.url)
      && config._sessionId !== (readSession()?.id ?? null)) throw new axios.CanceledError('La sesión cambió')
    return res
  },
  async (err) => {
    const original = err.config as SessionRequest | undefined
    if (typeof window === 'undefined' || !original || isSessionEndpoint(original.url)) throw err
    const session = readSession()
    if (original._sessionId !== (session?.id ?? null)) throw new axios.CanceledError('La sesión cambió')
    if (err.response?.status !== 401) throw err
    if (original._retry || !session?.refreshToken) {
      endSession(session?.id ?? null)
      throw err
    }
    original._retry = true
    // Una respuesta 401 tardía puede llegar después de una renovación exitosa.
    if (!session.accessToken || session.accessToken === original._accessToken) {
      await refreshSession(session, original._accessToken ?? null)
    }
    return api(original)
  }
)

export default api
