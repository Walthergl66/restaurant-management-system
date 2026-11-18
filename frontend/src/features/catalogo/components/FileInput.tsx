'use client'
import { useCallback, useRef, useState } from 'react'
import { Trash2, Upload } from 'lucide-react'

type FileInputProps = {
  accept?: string
  disabled?: boolean
  maxSizeMB?: number
  onChange: (file: File | null) => void
  value: File | null
}

export function FileInput({ accept = 'image/*,video/*', disabled = false, maxSizeMB = 50, onChange, value }: FileInputProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)

  const handleChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null
    if (!file) {
      onChange(null)
      setError(null)
      return
    }
    if (!file.type.startsWith('image/') && !file.type.startsWith('video/')) {
      setError('Solo se permiten imágenes o videos.')
      onChange(null)
      return
    }
    if (file.size > maxSizeMB * 1024 * 1024) {
      setError(`El archivo supera los ${maxSizeMB} MB.`)
      onChange(null)
      return
    }
    setError(null)
    onChange(file)
  }, [onChange, maxSizeMB])

  const clear = () => {
    if (inputRef.current) inputRef.current.value = ''
    onChange(null)
    setError(null)
  }

  const clickInput = () => {
    if (disabled) return
    inputRef.current?.click()
  }

  const areaClasses = `
    flex flex-col items-center justify-center gap-2 w-full min-h-[120px] rounded-lg border-2 border-dashed
    px-4 py-4 transition-colors text-sm
    ${disabled
      ? 'border-gray-200 bg-gray-50 text-gray-400 cursor-not-allowed hover:border-gray-200 hover:bg-gray-50'
      : 'border-gray-300 bg-transparent text-gray-600 hover:border-gray-400 hover:bg-gray-50/50 cursor-pointer'
    }
  `

  return (
    <div className="file-input-container">
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        disabled={disabled}
        onChange={handleChange}
        className="sr-only"
      />
      <button
        type="button"
        onClick={clickInput}
        disabled={disabled}
        className={areaClasses}
        aria-label="Seleccionar archivo"
      >
        <Upload size={20} strokeWidth={1.5} />
        {value ? 'Cambiar archivo' : 'Seleccionar archivo'}
      </button>

      {value && (
        <div className="mt-3 flex items-center justify-between px-3 py-2 rounded-lg bg-gray-50 border border-gray-100">
          <div className="flex items-center gap-2">
            <span className="text-sm text-gray-700 truncate max-w-[200px]" title={value.name}>{value.name}</span>
            <span className="text-xs text-gray-500">({Math.round(value.size / 1024)} KB)</span>
          </div>
          <button
            type="button"
            onClick={clear}
            className="text-xs text-red-500 hover:text-red-700 underline hover:underline"
            aria-label="Quitar archivo"
          >
            <Trash2 size={14} strokeWidth={2} /> Quitar
          </button>
        </div>
      )}

      {!value && !disabled && (
        <p className="mt-2 text-xs text-gray-500">Máximo {maxSizeMB} MB. Fotos o videos cortos.</p>
      )}

      {error && (
        <p className="mt-1 text-xs text-red-600">{error}</p>
      )}
    </div>
  )
}
