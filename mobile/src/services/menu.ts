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
  { id: 1, nombre: 'La Estación', descripcion: 'Doble carne, queso cheddar, bacon, huevo y nuestra secreta salsa de la casa', imagenUrl: 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400', precio: '8.99', categoriaId: 1, areaId: 1, activo: true, extras: demoExtras },
  { id: 2, nombre: 'Clásica Burger', descripcion: 'Carne Angus, queso, lechuga, tomate y salsa especial', imagenUrl: 'https://images.unsplash.com/photo-1571091718767-18b5b1457add?w=400', precio: '6.50', categoriaId: 1, areaId: 1, activo: true, extras: demoExtras },
  { id: 3, nombre: 'Veggie Power', descripcion: 'Hamburguesa de garbanzos, aguacate, tomate cherry y mayonesa vegana', imagenUrl: 'https://images.unsplash.com/photo-1525059696034-4967a729002e?w=400', precio: '7.25', categoriaId: 1, areaId: 1, activo: true, extras: demoExtras },
  { id: 4, nombre: 'Doble Bacon Cheese', descripcion: 'Doble carne, doble queso, doble bacon y cebolla caramelizada', imagenUrl: 'https://images.unsplash.com/photo-1553979459-d2229ba7433b?w=400', precio: '9.50', categoriaId: 1, areaId: 1, activo: true, extras: demoExtras },
  { id: 5, nombre: 'Papas Rústicas', descripcion: 'Papas gruesas con piel, romero y sal marina', imagenUrl: 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=400', precio: '3.25', categoriaId: 2, areaId: 1, activo: true, extras: [] },
  { id: 6, nombre: 'Aros de Cebolla', descripcion: 'Crujientes aros de cebolla con salsa chipotle', imagenUrl: 'https://images.unsplash.com/photo-1639024471283-03518883512d?w=400', precio: '3.75', categoriaId: 2, areaId: 1, activo: true, extras: [] },
  { id: 7, nombre: 'Nachos Supreme', descripcion: 'Nachos con queso fundido, guacamole, pico de gallo y crema', imagenUrl: 'https://images.unsplash.com/photo-1513456852971-30c0b8199d4d?w=400', precio: '4.50', categoriaId: 2, areaId: 1, activo: true, extras: [] },
  { id: 8, nombre: 'Limonada de Fresa', descripcion: 'Natural con fresas frescas y menta', imagenUrl: 'https://images.unsplash.com/photo-1437418747212-8d9709afab22?w=400', precio: '2.25', categoriaId: 3, areaId: 2, activo: true, extras: [] },
  { id: 9, nombre: 'Té Helado de Durazno', descripcion: 'Té negro con durazno natural', imagenUrl: 'https://images.unsplash.com/photo-1556679343-c7306c1976bc?w=400', precio: '2.00', categoriaId: 3, areaId: 2, activo: true, extras: [] },
  { id: 10, nombre: 'Malchoc Shake', descripcion: 'Batido de chocolate con malvaviscos', imagenUrl: 'https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=400', precio: '3.50', categoriaId: 3, areaId: 2, activo: true, extras: [] },
  { id: 11, nombre: 'Cheesecake de Frutos Rojos', descripcion: 'Suave cheesecake con coulis de frutos rojos', imagenUrl: 'https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=400', precio: '4.25', categoriaId: 4, areaId: 3, activo: true, extras: [] },
  { id: 12, nombre: 'Brownie con Helado', descripcion: 'Brownie tibio con vainilla y salsa de chocolate', imagenUrl: 'https://images.unsplash.com/photo-1606313564200-e75d5e30476c?w=400', precio: '3.95', categoriaId: 4, areaId: 3, activo: true, extras: [] },
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
