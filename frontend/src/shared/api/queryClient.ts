'use client'
import { QueryClient } from '@tanstack/react-query'
import axios from 'axios'
import { SessionUnavailableError } from './session'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        if (axios.isCancel(error) || error instanceof SessionUnavailableError) return false
        const status = axios.isAxiosError(error) ? error.response?.status : undefined
        // No repetir automáticamente consultas que requieren permiso, autenticación o corregir parámetros.
        return failureCount < 1 && (status == null || status >= 500)
      },
    },
  },
})
