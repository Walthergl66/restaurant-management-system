'use client'
import { useState } from 'react'
import { X, Plus, Minus, Trash2 } from 'lucide-react'
import { useMesasLibres, useProductosActivos, useCrearPedido } from '../hooks'
import type { CartItem } from '../types'
import type { Producto } from '@/features/catalogo/types'

export default function NuevoPedidoModal({ onClose }: { onClose: () => void }) {
  const [mesaId, setMesaId]     = useState<number | ''>('')
  const [notas, setNotas]       = useState('')
  const [confirmar, setConfirmar] = useState(true)
  const [cart, setCart]         = useState<CartItem[]>([])

  const { data: mesas = [] } = useMesasLibres()
  const { data: productos = [] } = useProductosActivos()

  const crear = useCrearPedido(onClose)

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
  const puedeCrear = mesaId !== '' && cart.length > 0 && !crear.isPending

  const crearPedido = () => {
    if (mesaId === '') return
    crear.mutate({
      mesaId: Number(mesaId),
      notas: notas || null,
      confirmar,
      items: cart.map(i => ({ productoId: i.productoId, cantidad: i.cantidad })),
    })
  }

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
                Mesa
              </label>
              <select
                value={mesaId}
                onChange={e => setMesaId(e.target.value === '' ? '' : Number(e.target.value))}
                className="input"
              >
                <option value="">Selecciona una mesa…</option>
                {mesas.map(m => (
                  <option key={m.id} value={m.id}>
                    Mesa {m.numero} · {m.capacidad}p{m.ubicacion ? ` · ${m.ubicacion}` : ''}
                  </option>
                ))}
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
                    {qty ? (
                      <span style={{ width:24, height:24, borderRadius:'50%', background:'var(--fuchsia)', color:'#fff', fontSize:12, fontWeight:700, display:'flex', alignItems:'center', justifyContent:'center' }}>
                        {qty}
                      </span>
                    ) : null}
                  </button>
                )
              })}
            </div>
          </div>

          {/* cart */}
          <div style={{ width:220, borderLeft:'1px solid var(--border)', display:'flex', flexDirection:'column', padding:16 }}>
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
                        <button
                          onClick={() => setCart(prev => prev.map(i => i.productoId === item.productoId ? { ...i, cantidad: i.cantidad + 1 } : i))}
                          style={{ width:22, height:22, borderRadius:'50%', background:'var(--surface-3)', border:'none', cursor:'pointer', display:'flex', alignItems:'center', justifyContent:'center', color:'var(--muted)' }}
                        >
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
              <input
                className="input"
                placeholder="Notas (opcional)"
                value={notas}
                onChange={e => setNotas(e.target.value)}
                style={{ marginBottom:8, fontSize:12 }}
              />
              <label style={{ display:'flex', alignItems:'center', gap:6, fontSize:12, color:'var(--muted)', marginBottom:10, cursor:'pointer' }}>
                <input type="checkbox" checked={confirmar} onChange={e => setConfirmar(e.target.checked)} />
                Confirmar y enviar a cocina
              </label>
              <div style={{ display:'flex', justifyContent:'space-between', fontSize:13, marginBottom:10 }}>
                <span style={{ color:'var(--muted)' }}>Total</span>
                <span style={{ color:'var(--yellow)', fontWeight:800 }}>${total.toFixed(2)}</span>
              </div>
              <button
                className="btn-yellow"
                onClick={crearPedido}
                disabled={!puedeCrear}
                style={{ width:'100%', justifyContent:'center', fontSize:12 }}
              >
                {crear.isPending ? 'Creando...' : confirmar ? 'Crear y confirmar' : 'Crear borrador'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
