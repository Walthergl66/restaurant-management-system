/**
 * Estación Burger — Tipos de pedidos del cliente
 * Contrato: ClientesController + SPI del backend (com.restaurante.clientes).
 *
 * - CrearPedidoClienteRequest / ConfirmarPedidoClienteRequest (requests)
 * - PedidoClienteSPI / CarritoClienteSPI / DireccionClienteSPI (respuestas)
 * - Page<T> de Spring Data (historial paginado)
 *
 * El backend serializa BigDecimal como número JSON (dinero siempre number).
 */

/** ExtraSPI — extra dentro de una línea de pedido. */
export interface ExtraLinea {
  extraId: number;
  nombre: string;
  precio: number;
}

/** LineaSPI — línea de pedido en la vista congelada. */
export interface LineaPedido {
  productoId: number;
  nombre: string;
  precio: number;
  cantidad: number;
  subtotal: number;
  extras: ExtraLinea[];
}

/**
 * PedidoClienteSPI — vista congelada del pedido.
 * Nunca expone idempotencyKey, clienteId ni direccionId.
 */
export interface PedidoCliente {
  codigo: string;
  estado: string;
  metodoPago: string;
  metodoEntrega: string;
  total: number;
  lineas: LineaPedido[];
  creadoAt: string;
  actualizadoAt: string;
  version: number;
}

/** CarritoClienteSPI — borradores del cliente con total congelado. */
export interface CarritoCliente {
  borradores: PedidoCliente[];
  total: number;
  totalItems: number;
}

/** ItemRequest — línea al crear el pedido. */
export interface ItemPedidoRequest {
  productoId: number;
  cantidad: number;
  extraIds?: number[];
  ingredientesRemovidos?: string[];
  observaciones?: string;
}

/** CrearPedidoClienteRequest — crear pedido en BORRADOR (idempotente). */
export interface CrearPedidoRequest {
  codigo: string;
  metodoPago: string;
  metodoEntrega: string;
  direccionId?: number | null;
  idempotencyKey: string;
  items: ItemPedidoRequest[];
}

/** ConfirmarPedidoClienteRequest — confirmar con la MISMA clave (RF-24/25). */
export interface ConfirmarPedidoRequest {
  idempotencyKey: string;
}

/** NuevaDireccionClienteRequest — alta de dirección para domicilio. */
export interface NuevaDireccionRequest {
  etiqueta: string;
  direccion: string;
  telefono?: string;
  observaciones?: string;
}

/** DireccionClienteSPI — dirección del cliente. */
export interface DireccionCliente {
  id: number;
  clienteId: number;
  etiqueta: string;
  direccion: string;
  telefono: string | null;
  observaciones: string | null;
  activa: boolean;
  creadoAt: string;
}

/** Page<T> de Spring Data (respuesta paginada del historial). */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
