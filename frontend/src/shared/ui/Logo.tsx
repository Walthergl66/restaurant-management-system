import Image from 'next/image'
import logo from '../../../public/Estacion-Burger-Logo.png'

export default function Logo({ size = 70 }: { size?: number }) {
  return (
    <span style={{ display: 'block', position: 'relative', width: size, maxWidth: '100%', aspectRatio: `${logo.width} / ${logo.height}` }}>
      <Image
        src={logo}
        alt="Estación Burger"
        fill
        sizes={`${size}px`}
        style={{ objectFit: 'contain' }}
        priority
      />
    </span>
  )
}
