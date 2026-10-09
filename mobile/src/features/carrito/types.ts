/**
 * Estación Burger — Tipos del carrito (estado local)
 */

import type { ExtraMenu, ProductoMenu } from '../menu/types';

/** Item del carrito: producto del menú + cantidad + extras elegidos. */
export interface CartItem {
  producto: ProductoMenu;
  cantidad: number;
  extras: ExtraMenu[];
  observaciones?: string;
}
