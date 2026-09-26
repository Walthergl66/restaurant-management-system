/**
 * Estación Burger — Servicio de Menú
 * Incluye modo demo con datos mock para ver el diseño sin backend
 */

import api from './api';
import { Categoria, Producto, Extra } from '../types';

const DEMO_MODE = true;

const demoExtras: Extra[] = [
  { id: 1, nombre: 'Queso cheddar', descripcion: 'Doble queso', precio: '1.50', activo: true },
  { id: 2, nombre: 'Bacon crujiente', descripcion: 'Tocino ahumado', precio: '2.00', activo: true },
  { id: 3, nombre: 'Huevo frito', descripcion: 'Huevo estrellado', precio: '1.00', activo: true },
  { id: 4, nombre: 'Aguacate', descripcion: 'Palta fresca', precio: '1.75', activo: true },
  { id: 5, nombre: 'Cebolla caramelizada', descripcion: '', precio: '0.75', activo: true },
  { id: 6, nombre: 'Jalapeños', descripcion: 'Picantes', precio: '0.50', activo: true },
];

const demoCategorias: Categoria[] = [
  { id: 1, nombre: 'Hamburguesas', descripcion: 'Las clásicas de siempre', orden: 1, activo: true },
  { id: 2, nombre: 'Acompañantes', descripcion: 'Complementos perfectos', orden: 2, activo: true },
  { id: 3, nombre: 'Bebidas', descripcion: 'Refrescantes y frías', orden: 3, activo: true },
  { id: 4, nombre: 'Postres', descripcion: 'Dulces finales', orden: 4, activo: true },
];

const demoProductos: Producto[] = [
  { id: 1, nombre: 'La Estación', descripcion: 'Doble carne, queso cheddar, bacon, huevo y nuestra secreta salsa de la casa', imagenUrl: null, precio: '8.99', categoriaId: 1, areaId: 1, activo: true, extras: demoExtras },
  { id: 2, nombre: 'Clásica Burger', descripcion: 'Carne Angus, queso, lechuga, tomate y salsa especial', imagenUrl: null, precio: '6.50', categoriaId: 1, areaId: 1, activo: true, extras: demoExtras },
  { id: 3, nombre: 'Veggie Power', descripcion: 'Hamburguesa de garbanzos, aguacate, tomate cherry y mayonesa vegana', imagenUrl: null, precio: '7.25', categoriaId: 1, areaId: 1, activo: true, extras: demoExtras },
  { id: 4, nombre: 'Doble Bacon Cheese', descripcion: 'Doble carne, doble queso, doble bacon y cebolla caramelizada', imagenUrl: null, precio: '9.50', categoriaId: 1, areaId: 1, activo: true, extras: demoExtras },
  { id: 5, nombre: 'Papas Rústicas', descripcion: 'Papas gruesas con piel, romero y sal marina', imagenUrl: null, precio: '3.25', categoriaId: 2, areaId: 1, activo: true, extras: [] },
  { id: 6, nombre: 'Aros de Cebolla', descripcion: 'Crujientes aros de cebolla con salsa chipotle', imagenUrl: null, precio: '3.75', categoriaId: 2, areaId: 1, activo: true, extras: [] },
  { id: 7, nombre: 'Nachos Supreme', descripcion: 'Nachos con queso fundido, guacamole, pico de gallo y crema', imagenUrl: null, precio: '4.50', categoriaId: 2, areaId: 1, activo: true, extras: [] },
  { id: 8, nombre: 'Limonada de Fresa', descripcion: 'Natural con fresas frescas y menta', imagenUrl: null, precio: '2.25', categoriaId: 3, areaId: 2, activo: true, extras: [] },
  { id: 9, nombre: 'Té Helado de Durazno', descripcion: 'Té negro con durazno natural', imagenUrl: null, precio: '2.00', categoriaId: 3, areaId: 2, activo: true, extras: [] },
  { id: 10, nombre: 'Malchoc Shake', descripcion: 'Batido de chocolate con malvaviscos', imagenUrl: null, precio: '3.50', categoriaId: 3, areaId: 2, activo: true, extras: [] },
  { id: 11, nombre: 'Cheesecake de Frutos Rojos', descripcion: 'Suave cheesecake con coulis de frutos rojos', imagenUrl: null, precio: '4.25', categoriaId: 4, areaId: 3, activo: true, extras: [] },
  { id: 12, nombre: 'Brownie con Helado', descripcion: 'Brownie tibio con vainilla y salsa de chocolate', imagenUrl: null, precio: '3.95', categoriaId: 4, areaId: 3, activo: true, extras: [] },
];

export const menuService = {
  async getMenu(): Promise<{ categorias: Categoria[]; productos: Producto[] }> {
    if (DEMO_MODE) {
      return { categorias: demoCategorias, productos: demoProductos };
    }
    return api.get('/menu');
  },

  async getCategorias(): Promise<Categoria[]> {
    if (DEMO_MODE) return demoCategorias;
    return api.get('/categorias');
  },

  async getProductos(): Promise<Producto[]> {
    if (DEMO_MODE) return demoProductos;
    return api.get('/productos');
  },

  async getProducto(id: number): Promise<Producto> {
    if (DEMO_MODE) {
      const p = demoProductos.find(p => p.id === id);
      if (!p) throw new Error('Producto no encontrado');
      return p;
    }
    return api.get(`/productos/${id}`);
  },
};
