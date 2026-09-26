/**
 * Estación Burger — Tipos TypeScript
 * Compatibles con el backend Spring Boot (PedidoClienteSPI, LineaSPI, ExtraSPI)
 */

export interface Producto {
  id: number;
  nombre: string;
  descripcion: string;
  imagenUrl: string | null;
  precio: string;
  categoriaId: number;
  categoriaNombre?: string;
  areaId: number;
  activo: boolean;
  extras?: Extra[];
}

export interface Extra {
  id: number;
  nombre: string;
  descripcion: string;
  precio: string;
  activo: boolean;
}

export interface Categoria {
  id: number;
  nombre: string;
  descripcion: string;
  orden: number;
  activo: boolean;
}

/**
 * PedidoClienteSPI — respuesta del backend
 * Fuente: com.restaurante.clientes.PedidoClienteSPI
 */
export interface PedidoCliente {
  codigo: string;
  estado: string;
  metodoPago: string;
  metodoEntrega: string;
  direccionId?: number;
  lineas: PedidoClienteLinea[];
  total?: number;
  creadoAt?: string;
  actualizadoAt?: string;
  version?: number;
}

/**
 * LineaSPI — línea de pedido en respuesta del backend
 */
export interface PedidoClienteLinea {
  id?: number;
  productoId: number;
  nombre: string;
  precio: number;
  cantidad: number;
  subtotal: number;
  observaciones?: string;
  extras?: PedidoClienteLineaExtra[];
}

/**
 * ExtraSPI — extra en línea de pedido
 */
export interface PedidoClienteLineaExtra {
  extraId: number;
  nombre: string;
  precio: number;
}

/**
 * Request para crear pedido (lo que el backend espera)
 * POST /api/v1/clientes/pedidos
 */
export interface CreatePedidoRequest {
  codigo: string;
  metodoPago: string;
  metodoEntrega: string;
  direccionId?: number;
  idempotencyKey: string;
  items: CreatePedidoItem[];
}

export interface CreatePedidoItem {
  productoId: number;
  cantidad: number;
  extraIds: number[];
  ingredientesRemovidos?: number[];
  observaciones?: string;
}

export interface DireccionCliente {
  id?: number;
  etiqueta: string;
  direccion: string;
  telefono?: string;
  observaciones?: string;
}

export interface Usuario {
  id: number;
  username: string;
  nombre: string;
  activo: boolean;
  rol: string;
  permisos: string[];
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds?: number;
  tokenType?: string;
  usuario: Usuario;
}

export interface CartItem {
  producto: Producto;
  cantidad: number;
  extras: Extra[];
  observaciones?: string;
}
