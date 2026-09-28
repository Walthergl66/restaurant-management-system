'use client'
import { useParams, useRouter } from 'next/navigation'
import { useQuery, useMutation } from '@tanstack/react-query'
import { ArrowLeft, Check, X } from 'lucide-react'
import api from '@/lib/api'
import { queryClient } from '@/lib/queryClient'
import { fmt, ESTADO_PEDIDO } from '@/lib/utils'
import type { PedidoResponse } from '@/types'
import toast from 'react-hot-toast'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'

export default function PedidoDetallePage() {
  const { codigo } = useParams<{ codigo: string }>()
  const router = useRouter()

  const { data: pedido, isLoading } = useQuery<PedidoResponse>({
    queryKey: ['pedido', codigo],
    queryFn: async () => { const { data } = await api.get(`/pedidos/${codigo}`); return data },
    enabled: !!codigo,
  })

  const confirmar = useMutation({
    mutationFn: () => api.post(`/pedidos/${codigo}/confirmar`, null, { headers: { 'Idempotency-Key': `web-${codigo}-${Date.now()}` } }),
    onSuccess: () => { toast.success('Confirmado'); queryClient.invalidateQueries({ queryKey: ['pedido', codigo] }) },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  const cancelar = useMutation({
    mutationFn: () => api.delete(`/pedidos/${codigo}`),
    onSuccess: () => { toast.success('Cancelado'); router.push('/pedidos') },
    onError: (e: any) => toast.error(e?.response?.data?.mensaje ?? 'Error'),
  })

  if (isLoading) return <p style={{ color:'var(--dim)', padding:32 }}>Cargando…</p>
  if (!pedido) return <p style={{ color:'var(--dim)', padding:32 }}>Pedido no encontrado</p>

  return (
    <div style={{ display:'flex', flexDirection:'column', gap:16, maxWidth:700 }}>
      <button onClick={() => router.push('/pedidos')} style={{ display:'flex', alignItems:'center', gap:6, fontSize:13, color:'var(--muted)', background:'none', border:'none', cursor:'pointer', width:'fit-content' }}>
        <ArrowLeft size={15}/> Volver
      </button>

      {/* cabecera */}
      <div className="card">
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', flexWrap:'wrap', gap:12 }}>
          <div>
            <p style={{ fontSize:11, color:'var(--muted)', textTransform:'uppercase', letterSpacing:1 }}>Código</p>
            <h1 style={{ fontSize:26, fontWeight:900, color:'var(--fuchsia)', marginTop:2 }}>#{pedido.codigo}</h1>
            <p style={{ fontSize:13, color:'var(--muted)', marginTop:4 }}>
              Mesa: <span style={{ color:'var(--text)', fontWeight:600 }}>{pedido.mesaNombre ?? 'Sin mesa'}</span>
              {pedido.creadoPor && <> · Por <span style={{ color:'var(--text)', fontWeight:600 }}>{pedido.creadoPor}</span></>}
            </p>
            {pedido.creadoAt && (
              <p style={{ fontSize:12, color:'var(--dim)', marginTop:2 }}>
                {format(new Date(pedido.creadoAt), "EEEE d 'de' MMMM · HH:mm", { locale: es })}
              </p>
            )}
          </div>
          <span className={`badge ${ESTADO_PEDIDO[pedido.estado]?.badge ?? ''}`} style={{ padding:'6px 14px', fontSize:12 }}>
            {ESTADO_PEDIDO[pedido.estado]?.label ?? pedido.estado}
          </span>
        </div>

        {pedido.estado === 'BORRADOR' && (
          <div style={{ display:'flex', gap:8, marginTop:16 }}>
            <button className="btn-yellow" onClick={() => confirmar.mutate()} disabled={confirmar.isPending}>
              <Check size={14}/> Confirmar
            </button>
            <button className="btn-ghost" style={{ color:'var(--error)' }} onClick={() => cancelar.mutate()} disabled={cancelar.isPending}>
              <X size={14}/> Cancelar
            </button>
          </div>
        )}
      </div>

      {/* líneas */}
      <div className="card">
        <h2 style={{ fontWeight:700, fontSize:15, marginBottom:14 }}>Ítems</h2>
        {!pedido.lineas?.length
          ? <p style={{ color:'var(--dim)', fontSize:13 }}>Sin ítems</p>
          : <>
              {pedido.lineas.map(l => (
                <div key={l.id} style={{ display:'flex', justifyContent:'space-between', alignItems:'center', padding:'11px 0', borderBottom:'1px solid var(--border)' }}>
                  <div>
                    <p style={{ fontSize:14, fontWeight:600 }}>{l.productoNombre}</p>
                    {l.anotaciones && <p style={{ fontSize:12, color:'var(--muted)', marginTop:2 }}>{l.anotaciones}</p>}
                  </div>
                  <div style={{ textAlign:'right' }}>
                    <p style={{ fontSize:12, color:'var(--muted)' }}>{l.cantidad} × {fmt(l.precioUnitario)}</p>
                    <p style={{ fontSize:14, fontWeight:700, color:'var(--yellow)' }}>{fmt(l.subtotal)}</p>
                  </div>
                </div>
              ))}
              <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center', paddingTop:14, marginTop:4 }}>
                <span style={{ fontWeight:700, fontSize:15 }}>Total</span>
                <span style={{ fontSize:26, fontWeight:900, color:'var(--yellow)' }}>{fmt(pedido.total)}</span>
              </div>
            </>
        }
      </div>
    </div>
  )
}
