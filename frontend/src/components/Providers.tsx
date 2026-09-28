'use client'
import { QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'react-hot-toast'
import { queryClient } from '@/lib/queryClient'

export default function Providers({ children }: { children: React.ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <Toaster
        position="top-right"
        toastOptions={{
          style: {
            background: '#1a1a1a',
            color: '#fff',
            border: '1px solid #2a2a2a',
            borderRadius: '10px',
            fontSize: '14px',
          },
          success: { iconTheme: { primary: '#F5A623', secondary: '#000' } },
          error:   { iconTheme: { primary: '#E91E8C', secondary: '#fff' } },
        }}
      />
    </QueryClientProvider>
  )
}
