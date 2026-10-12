import { readFile } from 'node:fs/promises'
import { join } from 'node:path'
import { ImageResponse } from 'next/og'

export const size = { width: 128, height: 128 }
export const contentType = 'image/png'

export default async function Icon() {
  const logo = await readFile(join(process.cwd(), 'public/Estacion-Burger-Logo.png'))
  // El archivo mide 1678 × 937, con márgenes transparentes a los lados.
  // Encuadramos la marca en un lienzo cuadrado de 1200 × 1200 unidades,
  // manteniendo su escala uniforme y dejando aire alrededor del dibujo.
  const scale = size.width / 1200

  return new ImageResponse(
    <div
      style={{
        display: 'flex',
        width: '100%',
        height: '100%',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
        background: 'transparent',
      }}
    >
      {/* El encuadre recorta únicamente márgenes vacíos; no estira la marca. */}
      {/* eslint-disable-next-line @next/next/no-img-element -- ImageResponse renderiza HTML, no componentes next/image */}
      <img
        src={`data:image/png;base64,${logo.toString('base64')}`}
        width={1678 * scale}
        height={937 * scale}
        alt=""
        style={{ flexShrink: 0 }}
      />
    </div>,
    size,
  )
}
