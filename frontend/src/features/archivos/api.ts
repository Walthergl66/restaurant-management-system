'use client'
import api from '@/shared/api/client'

export interface ArchivoSubido {
  id: string
  nombre: string
  tipo: 'IMAGEN' | 'VIDEO'
  contenidoTipo: string
  tamano: number
  url: string
}

export async function subirArchivo(archivo: File, progreso: (porcentaje: number) => void, signal: AbortSignal): Promise<ArchivoSubido> {
  const body = new FormData()
  body.append('archivo', archivo)
  const { data } = await api.post<ArchivoSubido>('/archivos', body, {
    headers: { 'Content-Type': undefined }, signal, timeout: 180_000,
    onUploadProgress: event => { if (event.total) progreso(Math.round(event.loaded * 100 / event.total)) },
  })
  return data
}

export async function descartarArchivo(id: string): Promise<void> {
  await api.delete(`/archivos/${id}`, { timeout: 15_000 })
}
