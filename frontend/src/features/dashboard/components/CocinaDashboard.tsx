'use client'
import { useRouter } from 'next/navigation'
import { ChefHat, Clock, CheckCircle, ArrowRight } from 'lucide-react'
import { useComandas, useEnPreparacion, useListo } from '@/features/comandas'
import type { ComandaEstado, ComandaResponse } from '@/features/comandas'

const COLS: { estado: ComandaEstado; label: string; color: string }[] = [
  { estado: 'PENDIENTE',      label: 'Pendientes', color: 'var(--yellow)'  },
  { estado: 'EN_PREPARACION', label: 'Preparando', color: 'var(--fuchsia)' },
  { estado: 'LISTO',          label: 'Listos',     color: 'var(--success)' },
]

/* ================================================================ */
export default function CocinaDashboard() {
  const router = useRouter()
  const { data: comandas = [], isLoading } = useComandas()
  const enPrep = useEnPreparacion()
  const listo = useListo()

  const enMarcha = comandas.filter(c => c.estado !== 'LISTO')

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* action bar */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
        <button className="btn-fuchsia" onClick={() => router.push('/cocina')}>
          <ChefHat size={15} /> Ir a cocina
        </button>
      </div>

      {/* KPIs */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 14 }}>
        {COLS.map(c => (
          <div key={c.estado} className="card" style={{ textAlign: 'center' }}>
            <p style={{ fontSize: 30, fontWeight: 900, color: c.color }}>
              {isLoading ? '...' : comandas.filter(x => x.estado === c.estado).length}
            </p>
            <p style={{ fontSize: 12, color: 'var(--muted)', marginTop: 2 }}>{c.label}</p>
          </div>
        ))}
      </div>

      {/* comandas en marcha */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <h2 style={{ fontWeight: 700, fontSize: 15 }}>Comandas en marcha</h2>
          <button onClick={() => router.push('/cocina')} style={{
            display: 'flex', alignItems: 'center', gap: 4, fontSize: 12,
            fontWeight: 600, color: 'var(--yellow)', background: 'none', border: 'none', cursor: 'pointer',
          }}>
            Ver todas <ArrowRight size={13} />
          </button>
        </div>

        {isLoading
          ? <p style={{ fontSize: 13, color: 'var(--dim)' }}>Cargando...</p>
          : enMarcha.length === 0
            ? <p style={{ fontSize: 13, color: 'var(--dim)' }}>No hay comandas pendientes</p>
            : <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill,minmax(260px,1fr))', gap: 12 }}>
                {enMarcha.map(cmd => (
                  <ComandaCard key={cmd.id} cmd={cmd} enPrep={enPrep} listo={listo} />
                ))}
              </div>
        }
      </div>
    </div>
  )
}

/* ---- tarjeta de comanda con acción rápida ---- */
function ComandaCard({ cmd, enPrep, listo }: {
  cmd: ComandaResponse
  enPrep: { mutate: (id: number) => void }
  listo: { mutate: (id: number) => void }
}) {
  const preparando = cmd.estado === 'EN_PREPARACION'
  return (
    <div style={{ background: 'var(--surface-2)', border: '1px solid var(--border)', borderRadius: 12, padding: 14 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
        <span style={{ fontSize: 13, fontWeight: 700, color: 'var(--fuchsia)' }}>#{cmd.pedidoCodigo}</span>
        <span style={{ fontSize: 11, color: 'var(--muted)' }}>Comanda #{cmd.numeroComanda}</span>
      </div>
      <p style={{ fontSize: 11, color: 'var(--muted)', marginBottom: 8 }}>{cmd.areaNombre}</p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 3, marginBottom: 10 }}>
        {cmd.lineas.slice(0, 3).map((item, i) => (
          <span key={i} style={{ fontSize: 12 }}><strong>{item.cantidad}×</strong> {item.nombreProducto}</span>
        ))}
      </div>
      {preparando
        ? <button style={{ width: '100%', justifyContent: 'center', fontSize: 11, padding: '6px 8px', background: 'var(--success)', color: '#000', border: 'none', borderRadius: 8, fontWeight: 700, cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4 }} onClick={() => listo.mutate(cmd.id)}>
            <CheckCircle size={11} /> Listo
          </button>
        : <button className="btn-fuchsia" style={{ width: '100%', justifyContent: 'center', fontSize: 11, padding: '6px 8px' }} onClick={() => enPrep.mutate(cmd.id)}>
            <Clock size={11} /> Preparar
          </button>
      }
    </div>
  )
}
