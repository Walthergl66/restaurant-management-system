import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'Estación Burger',
  description: 'Panel de administración Estación Burger',
  icons: {
    icon: { url: '/icon?v=3', type: 'image/png', sizes: '128x128' },
    apple: { url: '/icon?v=3', type: 'image/png', sizes: '128x128' },
  },
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link
          href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&display=swap"
          rel="stylesheet"
        />
      </head>
      <body>{children}</body>
    </html>
  )
}
