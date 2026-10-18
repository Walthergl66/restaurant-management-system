'use client'
import { useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Minus, Plus, Search, X } from 'lucide-react'
import { fmt } from '@/shared/lib/utils'
import { useCrearPedido, useMesasLibres, useProductosActivos } from '../hooks'
import type { CartItem } from '../types'

const labelStyle = {
  fontSize: 11, fontWeight: 700, color: 'var(--muted)',
  letterSpacing: 1, textTransform: 'uppercase', display: 'block', marginBottom: 5,
} as const

export default function NuevoPedidoModal({ onClose }: { onClose: () => void }) {
  const router = useRouter()
  const [mesaId, setMesaId] = useState('')
  const [notas, setNotas] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [carrito, setCarrito] = useState<CartItem[]>([])

  const { data: mesasLibres = [], isLoading: cargandoMesas } = useMesasLibres()
  const { data: productos = [], isLoading: cargandoProductos } = useProductosActivos()

  const crear = useCrearPedido(codigo => { onClose(); router.push(`/pedidos/${codigo}`) })

  const productosVisibles = useMemo(() => {
    const texto = busqueda.trim().toLowerCase()
    return productos.filter(p => !texto || p.nombre.toLowerCase().includes(texto))
  }, [productos, busqueda])

  const total = carrito.reduce((suma, i) => suma + i.precio * i.cantidad, 0)
  const listo = mesaId !== '' && carrito.length > 0 && !crear.isPending

  const agregar = (productoId: number, nombre: string, precio: number) =>
    setCarrito(actual =>
      actual.some(i => i.productoId === productoId)
        ? actual.map(i => (i.productoId === productoId ? { ...i, cantidad: i.cantidad + 1 } : i))
        : [...actual, { productoId, nombre, precio, cantidad: 1 }],
    )

  const cambiarCantidad = (productoId: number, delta: number) =>
    setCarrito(actual =>
      actual
        .map(i => (i.productoId === productoId ? { ...i, cantidad: i.cantidad + delta } : i))
        .filter(i => i.cantidad > 0),
    )

  const guardar = (confirmar: boolean) => {
    if (!listo) return
    crear.mutate({
      mesaId: Number(mesaId),
      notas: notas.trim() || null,
      confirmar,
      items: carrito.map(i => ({ productoId: i.productoId, cantidad: i.cantidad })),
    })
  }

  return (
    <div className="overlay" role="dialog" aria-modal="true" aria-labelledby="nuevo-pedido-title">
      <div className="admin-modal" style={{ maxWidth: 560 }}>
        <div className="modal-header">
          <div>
            <h2 id="nuevo-pedido-title">Nuevo pedido</h2>
            <p>Selecciona la mesa y agrega los ítems del pedido</p>
          </div>
          <button onClick={onClose} disabled={crear.isPending} aria-label="Cerrar" style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--muted)' }}>
            <X size={18} />
          </button>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <div>
              <label style={labelStyle} htmlFor="nuevo-pedido-mesa">MESA</label>
              <select id="nuevo-pedido-mesa" className="input" value={mesaId} onChange={e => setMesaId(e.target.value)} disabled={cargandoMesas || crear.isPending}>
                <option value="">
                  {cargandoMesas ? 'Cargando mesas…' : mesasLibres.length ? 'Selecciona una mesa…' : 'Sin mesas libres'}
                </option>
                {mesasLibres.map(m => (
                  <option key={m.id} value={m.id}>
                    Mesa {m.numero} · {m.capacidad} pers.{m.ubicacion ? ` · ${m.ubicacion}` : ''}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label style={labelStyle} htmlFor="nuevo-pedido-notas">NOTAS (OPCIONAL)</label>
              <input id="nuevo-pedido-notas" className="input" value={notas} maxLength={120} placeholder="Ej: terraza, cumpleaños…" onChange={e => setNotas(e.target.value)} disabled={crear.isPending} />
            </div>
          </div>

          <div>
            <label style={labelStyle} htmlFor="nuevo-pedido-busqueda">AGREGAR ÍTEMS</label>
            <div className="input" style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px' }}>
              <Search size={15} color="var(--muted)" />
              <input id="nuevo-pedido-busqueda" value={busqueda} onChange={e => setBusqueda(e.target.value)} placeholder="Buscar producto…" style={{ border: 'none', background: 'none', padding: 0, flex: 1, color: 'var(--text)' }} />
            </div>
            <div style={{ maxHeight: 176, overflowY: 'auto', border: '1px solid var(--border)', borderRadius: 12, marginTop: 6 }}>
              {productosVisibles.length === 0 ? (
                <p style={{ padding: 14, fontSize: 12, color: 'var(--dim)', textAlign: 'center' }}>
                  {cargandoProductos ? 'Cargando productos…' : 'Sin productos disponibles'}
                </p>
              ) : (
                productosVisibles.map(p => (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => agregar(p.id, p.nombre, p.precio)}
                    disabled={crear.isPending}
                    style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8, padding: '9px 12px', background: 'none', border: 'none', borderBottom: '1px solid var(--border)', cursor: 'pointer', textAlign: 'left' }}
                  >
                    <span>
                      <span style={{ fontSize: 13, fontWeight: 600, color: 'var(--text)', display: 'block' }}>{p.nombre}</span>
                      {p.categoriaNombre && <span style={{ fontSize: 11, color: 'var(--muted)' }}>{p.categoriaNombre}</span>}
                    </span>
                    <span style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <span style={{ fontSize: 12, fontWeight: 700, color: 'var(--yellow)' }}>{fmt(p.precio)}</span>
                      <Plus size={14} color="var(--fuchsia)" />
                    </span>
                  </button>
                ))
              )}
            </div>
          </div>

          <div>
            <label style={labelStyle}>ÍTEMS DEL PEDIDO ({carrito.length})</label>
            {carrito.length === 0 ? (
              <p style={{ fontSize: 12, color: 'var(--dim)' }}>Sin ítems aún — toca un producto para agregarlo.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                {carrito.map(i => (
                  <div key={i.productoId} style={{ display: 'flex', alignItems: 'center', gap: 10, background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: 10, padding: '7px 10px' }}>
                    <span style={{ flex: 1, fontSize: 13, fontWeight: 600 }}>{i.nombre}</span>
                    <span style={{ fontSize: 12, color: 'var(--muted)' }}>{fmt(i.precio)}</span>
                    <button className="btn-ghost" style={{ padding: '4px 8px' }} onClick={() => cambiarCantidad(i.productoId, -1)} disabled={crear.isPending} aria-label={`Quitar uno de ${i.nombre}`}><Minus size={12} /></button>
                    <span style={{ fontSize: 13, fontWeight: 700, minWidth: 18, textAlign: 'center' }}>{i.cantidad}</span>
                    <button className="btn-ghost" style={{ padding: '4px 8px' }} onClick={() => cambiarCantidad(i.productoId, 1)} disabled={crear.isPending} aria-label={`Agregar uno de ${i.nombre}`}><Plus size={12} /></button>
                    <span style={{ fontSize: 13, fontWeight: 800, color: 'var(--yellow)', minWidth: 68, textAlign: 'right' }}>{fmt(i.precio * i.cantidad)}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        <div className="modal-actions" style={{ justifyContent: 'space-between' }}>
          <span style={{ fontSize: 13, color: 'var(--muted)' }}>
            Total <strong style={{ fontSize: 17, fontWeight: 900, color: 'var(--yellow)' }}>{fmt(total)}</strong>
            {!listo && !crear.isPending && (mesaId === '' || carrito.length === 0) && (
              <span style={{ display: 'block', fontSize: 11, marginTop: 2 }}>Necesitas una mesa y al menos un ítem.</span>
            )}
          </span>
          <span style={{ display: 'flex', gap: 8 }}>
            <button className="btn-ghost" onClick={onClose} disabled={crear.isPending}>Cancelar</button>
            <button className="btn-fuchsia" onClick={() => guardar(false)} disabled={!listo} title={listo ? undefined : 'Selecciona una mesa y agrega al menos un ítem'}>
              Crear borrador
            </button>
            <button className="btn-yellow" onClick={() => guardar(true)} disabled={!listo} title={listo ? undefined : 'Selecciona una mesa y agrega al menos un ítem'}>
              {crear.isPending ? 'Creando…' : 'Crear y confirmar'}
            </button>
          </span>
        </div>
      </div>
    </div>
  )
}
