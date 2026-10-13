/**
 * Estación Burger — Tipos del carrito (estado local)
 */

import type { ExtraMenu, ProductoMenu } from '@/features/menu';

/** Item del carrito: producto del menú + cantidad + extras elegidos. */
export interface CartItem {
  producto: ProductoMenu;
  cantidad: number;
  extras: ExtraMenu[];
  ingredientesRemovidos: string[];
  observaciones?: string;
}
