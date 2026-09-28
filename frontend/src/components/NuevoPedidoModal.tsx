'use client'
import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { X, Plus, Minus, Trash2 } from 'lucide-react'
import api from '@/lib/api'
import { queryClient } from '@/lib/queryClient'
import type { Mesa, Producto } from '@/types'
import toast from 'react-hot-toast'

interface CartItem { productoId: number; nombre: string; precio: number; cantidad: number }

export default function NuevoPedidoModal({ onClose }: { onClose: () => void }) {
  const [mesaId, setMesaId] = useState<number | ''>('')
  const [cart, setCart]     = useState<CartItem[]>([])

  const { data: mesas = [] } = useQuery<Mesa[]>({
    queryKey: ['mesas', 'disponibles'],
    queryFn: async () => {
      const { data } = await api.get('/mesas')
      return (data as Mesa[]).filter(m => m.estado === 'DISPONIBLE')
    },
  })

  const { data: productos = [] } = useQuery<Producto[]>({
    queryKey: ['productos', 'activos'],
    queryFn: async () => {
      const { data } = await api.get('/productos')
      return (data as Producto[]).filter(p => p.activo && p.disponible)
    },
  })

  const crear = useMutation({
    mutationFn: async () => {
      const body: Record<string, unknown> = {
        lineas: cart.map(i => ({ productoId: i.productoId, cantidad: i.cantidad })),
      }
      if (mesaId !== '') body.mesaId = mesaId
      const { data } = await api.post('/pedidos', body)
      return data
    },
    onSuccess: (data) => {
      toast.success(`Pedido ${data.codigo} creado`)
      queryClient.invalidateQueries({ queryKey: ['pedidos'] })
      onClose()
    },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error al crear pedido'),
  })

  const add = (p: Producto) =>
    setCart(prev => {
      const ex = prev.find(i => i.productoId === p.id)
      if (ex) return prev.map(i => i.productoId === p.id ? { ...i, cantidad: i.cantidad + 1 } : i)
      return [...prev, { productoId: p.id, nombre: p.nombre, precio: p.precio, cantidad: 1 }]
    })

  const dec = (id: number) =>
    setCart(prev => {
      const ex = prev.find(i => i.productoId === id)
      if (!ex) return prev
      if (ex.cantidad === 1) return prev.filter(i => i.productoId !== id)
      return prev.map(i => i.productoId === id ? { ...i, cantidad: i.cantidad - 1 } : i)
    })

  const total = cart.reduce((s, i) => s + i.precio * i.cantidad, 0)

  return (
    <div className="overlay">
      <div style={{
        width: '100%', maxWidth: 680, background: 'var(--surface)',
        border: '1px solid var(--border)', borderRadius: 20,
        display: 'flex', flexDirection: 'column', maxHeight: '88vh',
      }}>
        {/* header */}
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'16px 20px', borderBottom:'1px solid var(--border)' }}>
          <h2 style={{ fontWeight:700, fontSize:16 }}>Nuevo Pedido</h2>
          <button onClick={onClose} style={{ background:'none', border:'none', cursor:'pointer', color:'var(--muted)' }}>
            <X size={20} />
          </button>
        </div>

        <div style={{ display:'flex', flex:1, minHeight:0, overflow:'hidden' }}>
          {/* productos */}
          <div style={{ flex:1, overflowY:'auto', padding:20 }}>
            <div style={{ marginBottom:16 }}>
              <label style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', display:'block', marginBottom:6 }}>
                Mesa (opcional)
              </label>
              <select
                value={mesaId}
                onChange={e => setMesaId(e.target.value === '' ? '' : Number(e.target.value))}
                className="input"
              >
                <option value="">Sin mesa (para llevar)</option>
                {mesas.map(m => <option key={m.id} value={m.id}>{m.nombre}</option>)}
              </select>
            </div>

            <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:8 }}>
              {productos.map(p => {
                const qty = cart.find(i => i.productoId === p.id)?.cantidad
                return (
                  <button
                    key={p.id}
                    onClick={() => add(p)}
                    style={{
                      background:'var(--surface-2)',
                      border:`1px solid ${qty ? 'var(--yellow)' : 'var(--border)'}`,
                      borderRadius:12, padding:'12px 14px',
                      display:'flex', justifyContent:'space-between', alignItems:'center',
                      cursor:'pointer', textAlign:'left',
                    }}
                  >
                    <div>
                      <p style={{ fontSize:13, fontWeight:600, color:'var(--text)' }}>{p.nombre}</p>
                      <p style={{ fontSize:12, color:'var(--yellow)', fontWeight:700, marginTop:2 }}>${p.precio.toFixed(2)}</p>
                    </div>
                    {qty && (
                      <span style={{ width:24, height:24, borderRadius:'50%', background:'var(--fuchsia)', color:'#fff', fontSize:12, fontWeight:700, display:'flex', alignItems:'center', justifyContent:'center' }}>
                        {qty}
                      </span>
                    )}
                  </button>
                )
              })}
            </div>
          </div>

          {/* cart */}
          <div style={{ width:210, borderLeft:'1px solid var(--border)', display:'flex', flexDirection:'column', padding:16 }}>
            <p style={{ fontSize:11, fontWeight:700, color:'var(--muted)', letterSpacing:1, textTransform:'uppercase', marginBottom:10 }}>Carrito</p>
            <div style={{ flex:1, overflowY:'auto', display:'flex', flexDirection:'column', gap:10 }}>
              {cart.length === 0
                ? <p style={{ fontSize:12, color:'var(--dim)', textAlign:'center', marginTop:16 }}>Agrega productos</p>
                : cart.map(item => (
                    <div key={item.productoId}>
                      <p style={{ fontSize:12, fontWeight:600 }}>{item.nombre}</p>
                      <div style={{ display:'flex', alignItems:'center', gap:4, marginTop:4 }}>
                        <button onClick={() => dec(item.productoId)} style={{ width:22, height:22, borderRadius:'50%', background:'var(--surface-3)', border:'none', cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', color:'var(--muted)' }}>
                          {item.cantidad === 1 ? <Trash2 size={10}/> : <Minus size={10}/>}
                        </button>
                        <span style={{ fontSize:12, fontWeight:700, width:18, textAlign:'center' }}>{item.cantidad}</span>
                        <button onClick={() => add({ ...p2(item) } as Producto)} style={{ width:22, height:22, borderRadius:'50%', background:'var(--surface-3)', border:'none', cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', color:'var(--muted)' }}>
                          <Plus size={10}/>
                        </button>
                        <span style={{ fontSize:11, color:'var(--yellow)', fontWeight:700, marginLeft:'auto' }}>
                          ${(item.precio * item.cantidad).toFixed(2)}
                        </span>
                      </div>
                    </div>
                  ))
              }
            </div>
            <div style={{ borderTop:'1px solid var(--border)', paddingTop:12, marginTop:12 }}>
              <div style={{ display:'flex', justifyContent:'space-between', fontSize:13, marginBottom:10 }}>
                <span style={{ color:'var(--muted)' }}>Total</span>
                <span style={{ color:'var(--yellow)', fontWeight:800 }}>${total.toFixed(2)}</span>
              </div>
              <button
                className="btn-yellow"
                onClick={() => crear.mutate()}
                disabled={cart.length === 0 || crear.isPending}
                style={{ width:'100%', justifyContent:'center', fontSize:12 }}
              >
                {crear.isPending ? 'Creando...' : 'Crear pedido'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

// helper to re-use the add function with a cart item
function p2(item: CartItem): Producto {
  return { id: item.productoId, nombre: item.nombre, precio: item.precio, activo: true, disponible: true, categoriaId: 0 }
}
