import Image from 'next/image'
import logo from '../../../public/Estacion-Burger-Logo.png'

export default function Logo({ size = 70 }: { size?: number }) {
  return (
    <Image
      src={logo}
      alt="Estación Burger"
      width={size}
      style={{ maxWidth: '100%', height: 'auto', objectFit: 'contain' }}
      priority
    />
  )
}
