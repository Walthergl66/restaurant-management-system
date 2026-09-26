/**
 * Estación Burger — Tipos TypeScript
 * Reflejan las entidades del backend
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

export interface PedidoCliente {
  codigo: string;
  estado: string;
  metodoPago: string;
  metodoEntrega: string;
  direccion?: DireccionCliente;
  lineas: PedidoClienteLinea[];
  total?: string;
  fechaCreacion?: string;
  observaciones?: string;
}

export interface PedidoClienteLinea {
  id?: number;
  productoId: number;
  nombreProducto: string;
  precioUnitario: string;
  cantidad: number;
  observaciones?: string;
  extras?: PedidoClienteLineaExtra[];
}

export interface PedidoClienteLineaExtra {
  extraId: number;
  nombreExtra: string;
  precio: string;
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
  usuario: Usuario;
}

export interface CartItem {
  producto: Producto;
  cantidad: number;
  extras: Extra[];
  observaciones?: string;
}
