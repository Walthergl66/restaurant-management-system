'use client'
import type { ButtonHTMLAttributes } from 'react'
import { GripVertical } from 'lucide-react'

export default function AsaCategoria(props: ButtonHTMLAttributes<HTMLButtonElement>) {
  return <button type="button" className="category-drag-handle" {...props}><GripVertical size={22}/></button>
}
