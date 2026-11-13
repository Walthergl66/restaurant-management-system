'use client'
import { useEffect, useMemo, useRef } from 'react'
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'

// Iconos por defecto de Leaflet: con bundlers los assets no resuelven solos.
const icono = L.icon({
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41],
})

interface Props {
  pos: { lat: number; lng: number } | null
  centroDefecto: { lat: number; lng: number }
  onSeleccionar: (lat: number, lng: number) => void
}

function CapturadorClick({ onSeleccionar }: { onSeleccionar: (lat: number, lng: number) => void }) {
  useMapEvents({
    click(e) {
      onSeleccionar(e.latlng.lat, e.latlng.lng)
    },
  })
  return null
}

function CentrarEn({ lat, lng }: { lat: number; lng: number }) {
  const map = useMap()
  const prev = useRef<string>('')
  useEffect(() => {
    const clave = `${lat},${lng}`
    if (prev.current !== clave) {
      prev.current = clave
      map.setView([lat, lng], map.getZoom())
    }
  }, [lat, lng, map])
  return null
}

export default function MapaSelectorUbicacion({ pos, centroDefecto, onSeleccionar }: Props) {
  const centro = useMemo(() => pos ?? centroDefecto, [pos, centroDefecto])

  return (
    <div style={{ height: 320, borderRadius: 12, overflow: 'hidden', border: '1px solid var(--border)' }}>
      <MapContainer
        center={[centro.lat, centro.lng]}
        zoom={pos ? 16 : 12}
        style={{ height: '100%', width: '100%' }}
        scrollWheelZoom
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <CapturadorClick onSeleccionar={onSeleccionar} />
        {pos && (
          <>
            <CentrarEn lat={pos.lat} lng={pos.lng} />
            <Marker
              position={[pos.lat, pos.lng]}
              icon={icono}
              draggable
              eventHandlers={{
                dragend(e) {
                  const m = e.target as L.Marker
                  const { lat, lng } = m.getLatLng()
                  onSeleccionar(lat, lng)
                },
              }}
            />
          </>
        )}
      </MapContainer>
    </div>
  )
}
