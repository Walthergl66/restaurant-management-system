'use client'
import { useState } from 'react'
import { ImageIcon, Pencil, Plus, Power, Search } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { hasPermiso } from '@/shared/lib/permissions'
import {
  useAreas, useCategorias, useHabilitacionCatalogo, useExtras, useProductos,
} from '../hooks'
import type { Area, Categoria, Extra, Producto } from '../types'
import CatalogoModal from './CatalogoModal'
import ConfirmDialog from './ConfirmDialog'
import ProductoModal from './ProductoModal'
import { useReordenCategorias } from './OrdenCategorias'

type Tab = 'productos' | 'categorias' | 'areas' | 'extras'
type CatalogItem = Categoria | Area | Extra

export default function ProductosScreen() {
  const [tab, setTab] = useState<Tab>('productos')
  const [search, setSearch] = useState('')
  const { user } = useAuthStore()
  const canManage = hasPermiso(user, 'catalogo:gestionar')
  return <div className="page-stack">
    <div className="page-header"><div><h1>Administración del catálogo</h1><p>Gestiona el menú que se publica en la aplicación móvil.</p></div></div>
    <div className="catalog-tabs" role="tablist">{([
      ['productos', 'Productos'], ['categorias', 'Categorías'], ['areas', 'Áreas de preparación'], ['extras', 'Extras'],
    ] as [Tab, string][]).map(([id, label]) => <button key={id} role="tab" aria-selected={tab === id} className={tab === id ? 'active' : ''} onClick={() => { setTab(id); setSearch('') }}>{label}</button>)}</div>
    <div className="catalog-toolbar"><div className="search-box"><Search size={16}/><input placeholder={`Buscar ${tab}…`} value={search} onChange={e => setSearch(e.target.value)}/></div></div>
    {tab === 'productos' ? <ProductsPanel search={search} canManage={canManage}/> : <CatalogPanel key={tab} kind={tab === 'categorias' ? 'categoria' : tab === 'areas' ? 'area' : 'extra'} search={search} canManage={canManage}/>}
  </div>
}

function ProductsPanel({ search, canManage }: { search: string; canManage: boolean }) {
  const [category, setCategory] = useState<number | 'TODOS'>('TODOS')
  const [status, setStatus] = useState<'TODOS' | 'ACTIVOS' | 'INACTIVOS'>('TODOS')
  const [editing, setEditing] = useState<Producto | null | undefined>(undefined)
  const [confirming, setConfirming] = useState<Producto | null>(null)
  const productosQuery = useProductos(); const categoriasQuery = useCategorias(); const areasQuery = useAreas(); const extrasQuery = useExtras()
  const habilitacion = useHabilitacionCatalogo('productos')
  const productos = productosQuery.data ?? []; const categorias = categoriasQuery.data ?? []; const areas = areasQuery.data ?? []; const extras = extrasQuery.data ?? []
  const filtered = productos.filter(p => (category === 'TODOS' || p.categoriaId === category) && (status === 'TODOS' || (status === 'ACTIVOS' ? p.activo : !p.activo)) && (!search || `${p.nombre} ${p.descripcion ?? ''}`.toLowerCase().includes(search.toLowerCase())))
  if (productosQuery.isLoading || categoriasQuery.isLoading || areasQuery.isLoading || extrasQuery.isLoading) return <StateCard text="Cargando catálogo…"/>
  if (productosQuery.isError || categoriasQuery.isError || areasQuery.isError || extrasQuery.isError) return <StateCard text="No se pudo cargar el catálogo. Revisa la conexión con el backend." error/>
  return <>
    <div className="filter-row"><select className="input filter-select" value={category} onChange={e => setCategory(e.target.value === 'TODOS' ? 'TODOS' : Number(e.target.value))}><option value="TODOS">Todas las categorías</option>{categorias.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}</select><select className="input filter-select" value={status} onChange={e => setStatus(e.target.value as typeof status)}><option value="TODOS">Todos los estados</option><option value="ACTIVOS">Activos</option><option value="INACTIVOS">Inactivos</option></select>{canManage && <button className="btn-fuchsia toolbar-action" onClick={() => setEditing(null)} disabled={!categorias.some(c => c.activo) || !areas.some(a => a.activo)}><Plus size={15}/>Nuevo producto</button>}</div>
    {!categorias.some(c => c.activo) || !areas.some(a => a.activo) ? <div className="notice-warning">Antes de crear productos necesitas al menos una categoría y un área de preparación activas.</div> : null}
    {!filtered.length ? <StateCard text="No hay productos que coincidan con los filtros."/> : <div className="product-admin-grid">{filtered.map(p => <article key={p.id} className={`product-admin-card${p.activo ? '' : ' inactive'}`}>
      <div className="product-admin-image">{p.imagenUrl ? <img // eslint-disable-line @next/next/no-img-element -- URL externa dinámica del gestor de archivos
        src={p.imagenUrl} alt=""/> : <ImageIcon size={30}/>}<span className={`status-pill ${p.activo ? 'active' : 'inactive'}`}>{p.activo ? 'Activo' : 'Inactivo'}</span></div>
      <div className="product-admin-body"><div className="product-meta"><span>{p.categoriaNombre ?? 'Sin categoría'}</span><span>{p.areaNombre ?? 'Sin área'}</span></div><h3>{p.nombre}</h3><p>{p.descripcion || 'Sin descripción'}</p><div className="product-stats"><strong>${Number(p.precio).toFixed(2)}</strong><span>{p.extras.length} extras · {p.ingredientes.length} ingredientes</span></div>{canManage && <div className="card-actions"><button className="btn-ghost" onClick={() => setEditing(p)}><Pencil size={13}/>Editar</button><button className={p.activo ? 'btn-ghost danger-text' : 'btn-yellow'} onClick={() => setConfirming(p)} disabled={habilitacion.isPending} aria-label={`${p.activo ? 'Desactivar' : 'Activar'} ${p.nombre}`}><Power size={14}/>{p.activo ? 'Desactivar' : 'Activar'}</button></div>}</div>
    </article>)}</div>}
    {editing !== undefined && <ProductoModal categorias={categorias} areas={areas} extras={extras} editing={editing} onClose={() => setEditing(undefined)}/>}
    {confirming && <ConfirmDialog activating={!confirming.activo} title={`${confirming.activo ? 'Desactivar' : 'Activar'} producto`} message={confirming.activo ? `“${confirming.nombre}” dejará de aparecer en la aplicación móvil. Su historial se conservará.` : `Se reactivará “${confirming.nombre}” conservando su ID y configuración. Su categoría debe estar activa para aparecer en el menú.`} pending={habilitacion.isPending} onClose={() => setConfirming(null)} onConfirm={() => habilitacion.mutate({ id: confirming.id, habilitada: !confirming.activo }, { onSuccess: () => setConfirming(null) })}/>}
  </>
}

function CatalogPanel({ kind, search, canManage }: { kind: 'categoria' | 'area' | 'extra'; search: string; canManage: boolean }) {
  const categoriasQuery = useCategorias(); const areasQuery = useAreas(); const extrasQuery = useExtras()
  const query = kind === 'categoria' ? categoriasQuery : kind === 'area' ? areasQuery : extrasQuery
  const orden = useReordenCategorias(categoriasQuery.data ?? [])
  const items = (kind === 'categoria' ? orden.visibles : query.data ?? []) as CatalogItem[]
  const [editing, setEditing] = useState<CatalogItem | null | undefined>(undefined)
  const [confirming, setConfirming] = useState<CatalogItem | null>(null)
  const habilitacion = useHabilitacionCatalogo(kind === 'categoria' ? 'categorias' : kind === 'area' ? 'areas' : 'extras')
  const filtered = items.filter(i => !search || `${i.nombre} ${i.descripcion ?? ''}`.toLowerCase().includes(search.toLowerCase()))
  const label = kind === 'categoria' ? 'categoría' : kind === 'area' ? 'área' : 'extra'
  if (query.isLoading) return <StateCard text="Cargando…"/>
  if (query.isError) return <StateCard text="No se pudieron cargar los datos." error/>
  return <>
    <div className="panel-heading"><div><strong>{filtered.length}</strong><span> registros</span></div>{canManage && <button className="btn-fuchsia" onClick={() => setEditing(null)}><Plus size={15}/>Nuevo {label}</button>}</div>
    {!filtered.length ? <StateCard text={`No hay ${label}s para mostrar.`}/> : <div className="card table-card"><div className="table-scroll"><table className="tbl"><thead><tr>{kind === 'categoria' && canManage && <th aria-label="Reordenar"/>}<th>Nombre</th><th>Descripción</th>{kind === 'extra' && <th>Precio</th>}<th>Estado</th>{canManage && <th>Acciones</th>}</tr></thead><tbody ref={kind === 'categoria' ? orden.lista : undefined}>{filtered.map(item => <tr key={item.id} data-categoria-id={kind === 'categoria' ? item.id : undefined} className={kind === 'categoria' && orden.arrastrando === item.id ? 'category-table-dragging' : undefined}>{kind === 'categoria' && canManage && <td className="category-handle-cell">{orden.asa(item as Categoria)}</td>}<td className="strong-cell">{item.nombre}</td><td>{item.descripcion || '—'}</td>{kind === 'extra' && <td>${Number((item as Extra).precio).toFixed(2)}</td>}<td><span className={`status-pill ${item.activo ? 'active' : 'inactive'}`}>{item.activo ? 'Activo' : 'Inactivo'}</span></td>{canManage && <td><div className="row-actions"><button className="icon-button" onClick={() => setEditing(item)} aria-label={`Editar ${item.nombre}`}><Pencil size={14}/></button><button className={item.activo ? 'btn-ghost danger-text' : 'btn-yellow'} onClick={() => setConfirming(item)} disabled={habilitacion.isPending} aria-label={`${item.activo ? 'Desactivar' : 'Activar'} ${item.nombre}`}><Power size={14}/>{item.activo ? 'Desactivar' : 'Activar'}</button></div></td>}</tr>)}</tbody></table></div></div>}
    {kind === 'categoria' && orden.ayuda}
    {editing !== undefined && <CatalogoModal kind={kind} editing={editing} onClose={() => setEditing(undefined)}/>}
    {confirming && <ConfirmDialog activating={!confirming.activo} title={`${confirming.activo ? 'Desactivar' : 'Activar'} ${label}`} message={confirming.activo ? `“${confirming.nombre}” dejará de estar disponible para nuevas configuraciones. Los datos históricos se conservarán.` : `Se reactivará “${confirming.nombre}” conservando el mismo ID y sus relaciones con los productos.`} pending={habilitacion.isPending} onClose={() => setConfirming(null)} onConfirm={() => habilitacion.mutate({ id: confirming.id, habilitada: !confirming.activo }, { onSuccess: () => setConfirming(null) })}/>}
  </>
}

function StateCard({ text, error = false }: { text: string; error?: boolean }) { return <div className={`state-card${error ? ' error' : ''}`}>{text}</div> }
