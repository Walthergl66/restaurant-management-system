'use client'
import { useEffect, useMemo, useState } from 'react'
import dynamic from 'next/dynamic'
import { MapPin, Save } from 'lucide-react'
import { useAuthStore, hasPermiso } from '@/features/auth'
import { useActualizarDireccion, useConfigRestaurante, useFijarCoordenadas } from '../hooks'

// Centro por defecto: Manabí, Ecuador (sin coordenadas confirmadas del local).
const CENTRO_DEFECTO = { lat: -0.95, lng: -80.58 }

// react-leaflet no soporta SSR: el mapa se carga solo en cliente.
const MapaSelectorUbicacion = dynamic(() => import('./MapaSelectorUbicacion'), {
  ssr: false,
  loading: () => (
    <div style={{ height: 320, borderRadius: 12, border: '1px solid var(--border)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--dim)' }}>
      Cargando mapa…
    </div>
  ),
})

export default function UbicacionRestaurantePanel() {
  const user = useAuthStore(s => s.user)
  const puedeGestionar = hasPermiso(user, 'delivery:config-gestionar')

  const { data: config, isLoading, isError } = useConfigRestaurante()
  const actualizarDireccion = useActualizarDireccion()
  const fijarCoordenadas = useFijarCoordenadas()

  const [direccion, setDireccion] = useState('')
  // Posición marcada en el mapa (puede diferir de la guardada hasta pulsar "Guardar ubicación").
  const [marcador, setMarcador] = useState<{ lat: number; lng: number } | null>(null)

  const coordsGuardadas = useMemo(
    () => (config?.lat != null && config?.lng != null ? { lat: config.lat, lng: config.lng } : null),
    [config?.lat, config?.lng],
  )

  useEffect(() => {
    setDireccion(config?.direccion ?? '')
  }, [config?.direccion])

  useEffect(() => {
    setMarcador(coordsGuardadas)
  }, [coordsGuardadas])

  if (!puedeGestionar) return null

  const direccionCambiada = direccion.trim() !== (config?.direccion ?? '').trim()
  const marcadorCambiado =
    marcador != null &&
    (coordsGuardadas == null ||
      marcador.lat !== coordsGuardadas.lat ||
      marcador.lng !== coordsGuardadas.lng)

  const guardarDireccion = () => {
    const d = direccion.trim()
    if (!d || actualizarDireccion.isPending) return
    actualizarDireccion.mutate(d)
  }

  const guardarUbicacion = () => {
    if (!marcador || fijarCoordenadas.isPending) return
    fijarCoordenadas.mutate(marcador)
  }

  return (
    <section style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <h2 style={{ fontSize: 16, fontWeight: 800 }}>Ubicación del restaurante</h2>

      {isLoading ? (
        <div className="card" style={{ padding: 32, textAlign: 'center', color: 'var(--dim)' }}>Cargando…</div>
      ) : isError ? (
        <div className="card" style={{ padding: 32, textAlign: 'center', color: 'var(--error)' }}>
          No se pudo cargar la configuración
        </div>
      ) : (
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Dirección */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            <label style={{ fontSize: 12, fontWeight: 600, color: 'var(--muted)' }}>
              Dirección del establecimiento
            </label>
            <div className="inline-control" style={{ flexWrap: 'wrap', alignItems: 'center' }}>
              <input
                className="input"
                style={{ flex: 1, minWidth: 240 }}
                value={direccion}
                onChange={e => setDireccion(e.target.value)}
                placeholder="Ej. 5 de Junio, Jaramijó, Manabí, Ecuador"
              />
              <button
                className="btn-fuchsia"
                onClick={guardarDireccion}
                disabled={!direccionCambiada || direccion.trim() === '' || actualizarDireccion.isPending}
                style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
              >
                <Save size={14} /> Guardar dirección
              </button>
            </div>
          </div>

          {/* Estado de coordenadas */}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 16, fontSize: 13 }}>
            <span style={{ color: 'var(--muted)' }}>
              Coordenadas:{' '}
              <strong style={{ color: 'var(--text)' }}>
                {coordsGuardadas
                  ? `${coordsGuardadas.lat.toFixed(6)}, ${coordsGuardadas.lng.toFixed(6)}`
                  : 'sin definir'}
              </strong>
            </span>
            <span style={{ color: 'var(--muted)' }}>
              Origen:{' '}
              <span className={`badge ${config?.origenCoords === 'MANUAL' ? 'badge-aprobada' : 'badge-confirmado'}`}>
                {config?.origenCoords ?? '—'}
              </span>
            </span>
          </div>

          {config?.requiereSeleccionManual && (
            <div
              role="alert"
              style={{
                display: 'flex', alignItems: 'center', gap: 8, padding: '10px 12px', borderRadius: 10,
                background: 'color-mix(in srgb, var(--yellow) 18%, transparent)',
                border: '1px solid var(--yellow)', color: 'var(--text)', fontSize: 13,
              }}
            >
              <MapPin size={16} />
              No se pudo ubicar automáticamente, selecciona el punto en el mapa.
            </div>
          )}

          {/* Mapa */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <span style={{ fontSize: 12, color: 'var(--dim)' }}>
              Haz clic en el mapa o arrastra el marcador para fijar la ubicación exacta.
            </span>
            <MapaSelectorUbicacion
              pos={marcador}
              centroDefecto={CENTRO_DEFECTO}
              onSeleccionar={(lat, lng) => setMarcador({ lat, lng })}
            />
            <div className="inline-control" style={{ flexWrap: 'wrap', alignItems: 'center' }}>
              <button
                className="btn-fuchsia"
                onClick={guardarUbicacion}
                disabled={!marcadorCambiado || fijarCoordenadas.isPending}
                style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}
              >
                <MapPin size={14} /> Guardar ubicación
              </button>
              {marcador && (
                <span style={{ fontSize: 12, color: 'var(--muted)' }}>
                  Marcador: {marcador.lat.toFixed(6)}, {marcador.lng.toFixed(6)}
                </span>
              )}
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
