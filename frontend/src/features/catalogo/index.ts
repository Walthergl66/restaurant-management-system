export { default as ProductosScreen } from './components/ProductosScreen'
export { default as ProductoModal } from './components/ProductoModal'
export {
  useProductos, useCategorias, useAreas, useExtras, useEliminarProducto, useGuardarProducto,
  useGuardarCategoria, useDesactivarCategoria, useGuardarArea, useDesactivarArea,
  useGuardarExtra, useDesactivarExtra,
} from './hooks'
export {
  listarProductos,
  listarCategorias,
  listarAreas,
  listarExtras,
  crearProducto,
  actualizarProducto,
  eliminarProducto,
} from './api'
export type { Area, Categoria, Extra, Producto, ProductoPayload } from './types'
