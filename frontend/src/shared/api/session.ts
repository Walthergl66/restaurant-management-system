'use client'
import axios from 'axios'

// Un único registro evita que otra pestaña lea una pareja de tokens a medio guardar.
export const SESSION_KEY = 'restaurant_session'
const FAILURE_KEY = 'restaurant_session_refresh_failure'
const ENDED_KEY = 'restaurant_session_ended'
const CHANGE_EVENT = 'restaurant-session-ended'
const UNAVAILABLE_EVENT = 'restaurant-session-unavailable'
const LOCK_NAME = 'restaurant-session-refresh'

export interface SessionTokens { accessToken: string; refreshToken: string }
export interface BrowserSession {
  id: string
  accessToken: string | null
  refreshToken: string | null
}

export class SessionUnavailableError extends Error {}

const transport = axios.create({ baseURL: '/api/v1', timeout: 15_000 })
let pendingRefresh: { id: string; promise: Promise<string> } | null = null

function applyEndMarker(session: BrowserSession | null): BrowserSession | null {
  const raw = localStorage.getItem(ENDED_KEY)
  if (raw) {
    try {
      const ended = JSON.parse(raw) as { id: string | null; nextId: string }
      if (typeof ended.nextId === 'string' && (!session || session.id === ended.id)) {
        return { id: ended.nextId, accessToken: null, refreshToken: null }
      }
    } catch { /* Ignorar una marca dañada. */ }
  }
  return session
}

export function readSession(): BrowserSession | null {
  if (typeof window === 'undefined') return null
  const raw = localStorage.getItem(SESSION_KEY)
  if (raw) {
    try {
      const value = JSON.parse(raw) as BrowserSession
      if (typeof value.id === 'string'
        && (value.accessToken === null || typeof value.accessToken === 'string')
        && (value.refreshToken === null || typeof value.refreshToken === 'string')) return applyEndMarker(value)
    } catch { /* Un registro dañado no acredita una sesión. */ }
    return null
  }
  // Compatibilidad con sesiones anteriores al cambio, sin confiar en el booleano de Zustand.
  const accessToken = localStorage.getItem('access_token')
  const refreshToken = localStorage.getItem('refresh_token')
  return applyEndMarker(accessToken || refreshToken ? { id: 'legacy', accessToken, refreshToken } : null)
}

function removeLegacySession() {
  for (const key of ['access_token', 'refresh_token', 'auth']) localStorage.removeItem(key)
}

export async function startSession(tokens: SessionTokens, expectedId: string | null): Promise<BrowserSession> {
  const save = () => {
    const previousEndMarker = localStorage.getItem(ENDED_KEY)
    if ((readSession()?.id ?? null) !== expectedId) throw new axios.CanceledError('La sesión cambió')
    const session = { id: crypto.randomUUID(), accessToken: tokens.accessToken, refreshToken: tokens.refreshToken }
    localStorage.setItem(SESSION_KEY, JSON.stringify(session))
    if (localStorage.getItem(ENDED_KEY) !== previousEndMarker) {
      endSession(session.id)
      throw new axios.CanceledError('La sesión cambió')
    }
    removeLegacySession()
    localStorage.removeItem(FAILURE_KEY)
    return session
  }
  // El alta de una sesión tampoco puede ser sobrescrita por un refresh de otra pestaña.
  return navigator.locks ? navigator.locks.request(LOCK_NAME, { signal: AbortSignal.timeout(25_000) }, save) : save()
}

export function endSession(expectedId: string | null = readSession()?.id ?? null) {
  if (typeof window === 'undefined' || (readSession()?.id ?? null) !== expectedId) return
  // La marca invalida inmediatamente el acceso en todas las pestañas, incluso si otra tiene el lock.
  const nextId = crypto.randomUUID()
  localStorage.setItem(ENDED_KEY, JSON.stringify({ id: expectedId, nextId }))
  removeLegacySession()
  localStorage.removeItem(FAILURE_KEY)
  window.dispatchEvent(new Event(CHANGE_EVENT))
  const clear = () => {
    if (readSession()?.id === nextId) {
      localStorage.setItem(SESSION_KEY, JSON.stringify({ id: nextId, accessToken: null, refreshToken: null }))
    }
  }
  if (navigator.locks) void navigator.locks.request(LOCK_NAME, clear).catch(() => {})
  else clear()
}

export function subscribeSession(listener: () => void, onUnavailable: (id: string, message: string) => void) {
  const storage = (event: StorageEvent) => {
    if (event.key === SESSION_KEY || event.key === ENDED_KEY || event.key === null) listener()
  }
  const unavailable = (event: Event) => {
    const { id, message } = (event as CustomEvent<{ id: string; message: string }>).detail
    onUnavailable(id, message)
  }
  window.addEventListener('storage', storage)
  window.addEventListener(CHANGE_EVENT, listener)
  window.addEventListener(UNAVAILABLE_EVENT, unavailable)
  return () => {
    window.removeEventListener('storage', storage)
    window.removeEventListener(CHANGE_EVENT, listener)
    window.removeEventListener(UNAVAILABLE_EVENT, unavailable)
  }
}

export async function revokeSession(tokens: SessionTokens) {
  await transport.post('/auth/logout', { refreshToken: tokens.refreshToken }, {
    headers: { Authorization: `Bearer ${tokens.accessToken}` },
  })
}

function checkCooldown(id: string) {
  const raw = localStorage.getItem(FAILURE_KEY)
  if (!raw) return
  try {
    const failure = JSON.parse(raw) as { id: string; retryAt: number; message: string }
    if (failure.id === id && failure.retryAt > Date.now()) throw new SessionUnavailableError(failure.message)
  } catch (error) {
    if (error instanceof SessionUnavailableError) throw error
  }
}

async function renewUnderLock(expected: BrowserSession, failedAccessToken: string | null) {
  const current = readSession()
  if (!current || current.id !== expected.id || !current.refreshToken) throw new axios.CanceledError('La sesión cambió')
  // Otra pestaña ya renovó mientras esperábamos el bloqueo.
  if (current.accessToken && current.accessToken !== failedAccessToken) return current.accessToken
  checkCooldown(current.id)
  try {
    const { data } = await transport.post<SessionTokens>('/auth/refresh', { refreshToken: current.refreshToken })
    if (typeof data.accessToken !== 'string' || !data.accessToken
      || typeof data.refreshToken !== 'string' || !data.refreshToken) {
      throw new SessionUnavailableError('No se pudo renovar la sesión. Intenta nuevamente.')
    }
    if (readSession()?.id !== current.id) {
      // Si se salió mientras el servidor renovaba, tampoco dejar su nuevo refresh activo.
      await revokeSession(data).catch(() => {})
      throw new axios.CanceledError('La sesión cambió')
    }
    localStorage.setItem(SESSION_KEY, JSON.stringify({ id: current.id, accessToken: data.accessToken, refreshToken: data.refreshToken }))
    removeLegacySession()
    localStorage.removeItem(FAILURE_KEY)
    return data.accessToken
  } catch (error) {
    if (readSession()?.id !== current.id || axios.isCancel(error)) throw error
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      endSession(current.id)
      throw error
    }
    const limited = axios.isAxiosError(error) && error.response?.status === 429
    const retryAfter = axios.isAxiosError(error) ? error.response?.headers['retry-after'] : undefined
    const seconds = retryAfter != null ? Number(retryAfter) : NaN
    const retryDate = retryAfter != null ? Date.parse(String(retryAfter)) : NaN
    const retryMs = Number.isFinite(seconds) ? Math.max(1000, seconds * 1000)
      : Number.isFinite(retryDate) ? Math.max(1000, retryDate - Date.now())
        : limited ? 60_000 : 5000
    const message = limited
      ? 'Se alcanzó el límite de intentos. Espera un momento antes de reintentar.'
      : 'No se pudo comprobar la sesión. Revisa la conexión e intenta nuevamente.'
    localStorage.setItem(FAILURE_KEY, JSON.stringify({ id: current.id, retryAt: Date.now() + retryMs, message }))
    throw new SessionUnavailableError(message)
  }
}

export function refreshSession(expected: BrowserSession, failedAccessToken: string | null): Promise<string> {
  if (pendingRefresh?.id === expected.id) return pendingRefresh.promise
  const promise = (async () => {
    // Web Locks coordina todas las pestañas del mismo origen; no enviar renovaciones sin exclusión.
    if (!navigator.locks) throw new SessionUnavailableError('No se pudo renovar la sesión. Vuelve a iniciar sesión.')
    return navigator.locks.request(LOCK_NAME, { signal: AbortSignal.timeout(25_000) }, () => renewUnderLock(expected, failedAccessToken))
  })().catch(error => {
    if (readSession()?.id === expected.id && !axios.isCancel(error)) {
      const message = error instanceof SessionUnavailableError ? error.message
        : 'No se pudo comprobar la sesión. Revisa la conexión e intenta nuevamente.'
      window.dispatchEvent(new CustomEvent(UNAVAILABLE_EVENT, { detail: { id: expected.id, message } }))
    }
    throw error
  })
  pendingRefresh = { id: expected.id, promise }
  void promise.finally(() => {
    if (pendingRefresh?.promise === promise) pendingRefresh = null
  }).catch(() => {})
  return promise
}
