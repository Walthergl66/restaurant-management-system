/** Contrato de ComandaResponse (GET /api/v1/comandas). Los campos nulos del backend se omiten del JSON. */

export type EstadoComanda = 'PENDIENTE' | 'EN_PREPARACION' | 'LISTO';
export type TipoComanda = 'ORDEN' | 'CANCELACION';
export type CanalComanda = 'SALON' | 'APP';

export interface LineaComanda {
  productoId: number;
  nombreProducto: string;
  cantidad: number;
  /** Extras elegidos, ya unidos en un texto. */
  extras?: string;
  /** Ingredientes removidos, ya unidos en un texto. */
  ingredientes?: string;
  observaciones?: string;
  orden: number;
}

export interface ComandaCocina {
  id: number;
  pedidoCodigo: string;
  numeroComanda: number;
  areaId: number;
  areaNombre: string;
  tipo: TipoComanda;
  estado: EstadoComanda;
  /** Número de mesa; solo en pedidos de salón. */
  mesaNumero?: number;
  canal: CanalComanda;
  /** Solo en pedidos de la app. */
  entrega?: 'RETIRAR' | 'DOMICILIO';
  lineas: LineaComanda[];
}

/** AreaResponse del catálogo: áreas de preparación (Cocina, Barra…). */
export interface AreaCocina {
  id: number;
  nombre: string;
  activo: boolean;
}

/** Acción que el usuario puede pulsar sobre una comanda. */
export type AccionComanda = 'PREPARAR' | 'LISTO' | 'ENTERADO';

export type ComandasPorEstado = Record<EstadoComanda, ComandaCocina[]>;
