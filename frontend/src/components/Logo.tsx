import Image from 'next/image'

export default function Logo({ size = 70 }: { size?: number }) {
  return (
    <Image
      src="/favicon.ico"
      alt="Estación Burger"
      width={size}
      height={size}
      unoptimized
      style={{ objectFit: 'contain' }}
    />
  )
}
