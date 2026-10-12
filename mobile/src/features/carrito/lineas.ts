import type { CartItem } from './types';

/** La personalización es parte de la identidad, independientemente del orden de selección. */
export function claveLinea(item: Pick<CartItem, 'producto' | 'extras' | 'ingredientesRemovidos'>): string {
  return JSON.stringify([
    item.producto.id,
    [...new Set(item.extras.map(extra => extra.id))].sort((a, b) => a - b),
    [...new Set(item.ingredientesRemovidos)].sort(),
  ]);
}

/** Contrato compartido por checkout de cliente y atención de mesas. */
export function aLineaPedido(item: CartItem) {
  return {
    productoId: item.producto.id,
    cantidad: item.cantidad,
    extraIds: item.extras.map(extra => extra.id),
    ingredientesRemovidos: item.ingredientesRemovidos,
    observaciones: item.observaciones,
  };
}
