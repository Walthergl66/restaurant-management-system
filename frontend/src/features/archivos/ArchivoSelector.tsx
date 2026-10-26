'use client'
import { useEffect, useId, useRef, useState } from 'react'
import { ImageIcon, Upload, Video, X } from 'lucide-react'

type Props = {
  tipo: 'IMAGEN' | 'VIDEO'
  archivo: File | null
  url: string | null
  disabled: boolean
  onChange: (archivo: File | null) => void
}

export default function ArchivoSelector({ tipo, archivo, url, disabled, onChange }: Props) {
  const id = useId()
  const input = useRef<HTMLInputElement>(null)
  const [local, setLocal] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [fallo, setFallo] = useState(false)
  const imagen = tipo === 'IMAGEN'
  const nombre = imagen ? 'imagen' : 'video'
  const maximo = imagen ? 10 : 100
  useEffect(() => () => { if (local) URL.revokeObjectURL(local) }, [local])
  const preview = archivo ? local : url
  const elegir = (file: File | undefined) => {
    if (!file) return
    const formatos = imagen ? ['image/jpeg', 'image/png', 'image/webp'] : ['video/mp4', 'video/webm']
    if (!formatos.includes(file.type)) { setError(`Formato no permitido. ${imagen ? 'Usa JPG, PNG o WebP.' : 'Usa MP4 o WebM.'}`); return }
    if (!file.size || file.size > maximo * 1024 * 1024) { setError(`El archivo debe tener contenido y pesar como máximo ${maximo} MB.`); return }
    setError(''); setFallo(false); setLocal(URL.createObjectURL(file)); onChange(file)
  }
  return <section className="media-upload" aria-labelledby={id}>
    <h3 id={id}>{imagen ? 'Imagen principal' : 'Video del producto'} {!imagen && <span className="field-hint">(opcional)</span>}</h3>
    <div className="image-preview media-preview">
      {preview && !fallo ? imagen
        // eslint-disable-next-line @next/next/no-img-element -- Archivo seleccionado o recurso del backend.
        ? <img src={preview} alt="Vista previa del producto" onError={() => setFallo(true)} />
        : <video src={preview} controls preload="metadata" playsInline onError={() => setFallo(true)} />
        : <>{imagen ? <ImageIcon size={32} /> : <Video size={32} />}<span>{fallo ? `No se pudo mostrar el ${nombre}` : `Sin ${nombre}`}</span></>}
    </div>
    {fallo && !imagen && <p className="field-error">El navegador no puede reproducir este video. Usa MP4 con H.264/AAC o WebM compatible.</p>}
    <input ref={input} type="file" hidden disabled={disabled}
      accept={imagen ? 'image/jpeg,image/png,image/webp' : 'video/mp4,video/webm'}
      aria-label={`Seleccionar ${nombre}`}
      onChange={event => { elegir(event.target.files?.[0]); event.target.value = '' }} />
    <div className="media-upload-actions">
      <button type="button" className="btn-ghost" disabled={disabled} onClick={() => input.current?.click()}><Upload size={14} />{preview ? `Cambiar ${nombre}` : `Seleccionar ${nombre}`}</button>
      {(archivo || url) && <button type="button" className="btn-ghost danger-text" disabled={disabled} onClick={() => { onChange(null); setError(''); setFallo(false); setLocal(null) }}><X size={14} />Quitar</button>}
    </div>
    {archivo && <p className="field-hint media-filename">{archivo.name} · {(archivo.size / 1024 / 1024).toFixed(2)} MB</p>}
    <p className="field-hint">{imagen ? 'JPG, PNG o WebP. Hasta 10 MB.' : 'MP4 o WebM. Hasta 100 MB.'} Se sube al guardar el producto.</p>
    {error && <p role="alert" className="field-error">{error}</p>}
  </section>
}
