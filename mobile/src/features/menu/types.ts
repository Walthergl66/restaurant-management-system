/**
 * Estación Burger — Tipos del menú público
 * Contrato: MenuController (GET /api/v1/menu, SIN autenticación, QR).
 *
 * MenuDto { restaurante, categorias[{ id, nombre, productos[] }] }
 */

/** InfoRestauranteDto — datos generales del restaurante. */
export interface InfoRestaurante {
  nombre: string;
  moneda: string;
}

/** ExtraResponse — extra disponible en un producto. */
export interface ExtraMenu {
  id: number;
  nombre: string;
  descripcion: string | null;
  precio: number;
  activo: boolean;
}

/** ProductoMenuDto — producto visible en el menú público. */
export interface ProductoMenu {
  id: number;
  nombre: string;
  descripcion: string | null;
  imagenUrl: string | null;
  precio: number;
  extras: ExtraMenu[];
  ingredientes: string[];
}

/** CategoriaMenuDto — categoría con sus productos activos. */
export interface CategoriaMenu {
  id: number;
  nombre: string;
  productos: ProductoMenu[];
}

/** MenuDto — respuesta completa del menú público. */
export interface MenuDto {
  restaurante: InfoRestaurante;
  categorias: CategoriaMenu[];
}
