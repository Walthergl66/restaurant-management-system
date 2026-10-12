export { default as ProductosScreen } from './components/ProductosScreen'
export { default as ProductoModal } from './components/ProductoModal'
export { useProductos, useCategorias, useEliminarProducto, useGuardarProducto } from './hooks'
export {
  listarProductos,
  listarCategorias,
  crearProducto,
  actualizarProducto,
  eliminarProducto,
} from './api'
export type { Categoria, Extra, Producto, ProductoPayload } from './types'
