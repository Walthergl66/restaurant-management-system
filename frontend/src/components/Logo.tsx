import Image from 'next/image'

export default function Logo({ size = 70 }: { size?: number }) {
  return (
    <Image
      src="/logo.png"
      alt="Estación Burger"
      width={size}
      height={size}
      style={{ objectFit: 'contain' }}
      priority
    />
  )
}
