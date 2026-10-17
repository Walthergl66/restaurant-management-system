'use client'
import axios from 'axios'

const api = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  if (typeof window !== 'undefined') {
    const token = localStorage.getItem('access_token')
    if (token) config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  async (err) => {
    const original = err.config
    const url = original?.url ?? ''
    const esLlamadaAuth = url.includes('/auth/login') || url.includes('/auth/refresh')
    const enLogin = typeof window !== 'undefined' && window.location.pathname.startsWith('/login')

    if (err.response?.status === 401 && original && !original._retry && !esLlamadaAuth) {
      original._retry = true
      const refresh = typeof window !== 'undefined' ? localStorage.getItem('refresh_token') : null
      if (refresh) {
        try {
          const { data } = await axios.post('/api/v1/auth/refresh', { refreshToken: refresh })
          localStorage.setItem('access_token', data.accessToken)
          localStorage.setItem('refresh_token', data.refreshToken)
          original.headers.Authorization = `Bearer ${data.accessToken}`
          return api(original)
        } catch {
          localStorage.clear()
          if (!enLogin) window.location.href = '/login'
        }
      } else {
        if (typeof window !== 'undefined') {
          localStorage.clear()
          if (!enLogin) window.location.href = '/login'
        }
      }
    }
    return Promise.reject(err)
  }
)

export default api
