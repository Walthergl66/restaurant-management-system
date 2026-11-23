/** Contratos que consume el mostrador (PedidoResponse y PedidoMostradorSPI del backend). */

/** PedidoResponse (GET /pedidos): solo lo que el mostrador usa. */
export interface PedidoSalonApi {
  codigo: string;
  mesaId?: number;
  estado: string;
  createdAt?: string;
  lineas: { nombreProducto: string; cantidad: number }[];
}

/** PedidoMostradorSPI (GET /clientes/operacion/pedidos). */
export interface PedidoRetiroApi {
  codigo: string;
  estado: string;
  entrega: 'RETIRAR' | 'DOMICILIO';
  clienteNombre?: string;
  lineas: { nombreProducto: string; cantidad: number }[];
}

export type OrigenMostrador = 'SALON' | 'RETIRO';

/** Pedido listo para entregar, ya sea servido en una mesa o recogido por el cliente. */
export interface PedidoMostrador {
  codigo: string;
  origen: OrigenMostrador;
  /** Número de la mesa (solo salón). */
  mesaNumero?: number;
  /** Nombre del cliente (solo retiro). */
  clienteNombre?: string;
  creadoAt?: string;
  lineas: { nombreProducto: string; cantidad: number }[];
}

/** Resultado de cargar el mostrador: lo que se pudo leer y los avisos de lo que no. */
export interface ResultadoMostrador {
  pedidos: PedidoMostrador[];
  avisos: string[];
}
