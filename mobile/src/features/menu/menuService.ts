/**
 * Estación Burger — Servicio de menú
 * Contrato: GET /api/v1/menu (público, sin autenticación — QR de mesa).
 *
 * El menú público agrupa productos por categoría; no existe endpoint
 * de detalle de un solo producto, así que se resuelve desde el menú.
 */

import { apiClient } from '../../core/api/apiClient';
import type { CategoriaMenu, MenuDto, ProductoMenu } from './types';

const ENDPOINTS = {
  menu: '/menu',
} as const;

export const menuService = {
  /** Menú público: datos del restaurante + categorías con productos. */
  async getMenu(): Promise<MenuDto> {
    return apiClient.get<MenuDto>(ENDPOINTS.menu);
  },

  /** Categorías del menú público. */
  async getCategorias(): Promise<CategoriaMenu[]> {
    const menu = await this.getMenu();
    return menu.categorias;
  },

  /** Todos los productos del menú (aplanados desde las categorías). */
  async getProductos(): Promise<ProductoMenu[]> {
    const menu = await this.getMenu();
    return menu.categorias.flatMap(categoria => categoria.productos);
  },

  /** Un producto por id (resuelto desde el menú público). */
  async getProducto(id: number): Promise<ProductoMenu> {
    const productos = await this.getProductos();
    const producto = productos.find(p => p.id === id);
    if (!producto) {
      throw new Error('Producto no encontrado');
    }
    return producto;
  },
};
