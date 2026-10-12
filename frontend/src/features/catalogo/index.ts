export { default as ProductosScreen } from './components/ProductosScreen'
export { default as ProductoModal } from './components/ProductoModal'
export {
  useProductos, useCategorias, useAreas, useExtras, useGuardarProducto,
  useGuardarCategoria, useGuardarArea, useGuardarExtra, useHabilitacionCatalogo,
} from './hooks'
export {
  listarProductos,
  listarCategorias,
  listarAreas,
  listarExtras,
  crearProducto,
  actualizarProducto,
} from './api'
export type { Area, Categoria, Extra, Producto, ProductoPayload } from './types'
