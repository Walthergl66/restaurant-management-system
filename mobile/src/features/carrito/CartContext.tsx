/**
 * Estación Burger — Contexto del carrito
 *
 * Estado local del carrito (UI). El envío al backend ocurre en el
 * checkout: carrito -> POST /clientes/pedidos (BORRADOR) -> confirmar.
 */

import React, {
  createContext,
  useCallback,
  useContext,
  useState,
  ReactNode,
} from 'react';
import type { CartItem } from './types';
import { claveLinea } from './lineas';
import type { ExtraMenu, ProductoMenu } from '@/features/menu';

interface CartContextType {
  items: CartItem[];
  totalItems: number;
  totalPrice: number;
  addItem: (producto: ProductoMenu, cantidad?: number, extras?: ExtraMenu[], ingredientesRemovidos?: string[]) => void;
  removeItem: (productoId: number, extras: ExtraMenu[], ingredientesRemovidos?: string[]) => void;
  updateQuantity: (
    productoId: number,
    extras: ExtraMenu[],
    cantidad: number,
    ingredientesRemovidos?: string[]
  ) => void;
  clearCart: () => void;
  getItemQuantity: (productoId: number) => number;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

const mismaCombinacion = (
  a: CartItem,
  productoId: number,
  extras: ExtraMenu[],
  ingredientesRemovidos: string[] = []
) =>
  claveLinea(a) === claveLinea({ producto: { ...a.producto, id: productoId }, extras, ingredientesRemovidos });

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);

  const addItem = useCallback(
    (producto: ProductoMenu, cantidad: number = 1, extras: ExtraMenu[] = [], ingredientesRemovidos: string[] = []) => {
      setItems(prev => {
        const existingIndex = prev.findIndex(item =>
          mismaCombinacion(item, producto.id, extras, ingredientesRemovidos)
        );

        if (existingIndex >= 0) {
          const updated = [...prev];
          updated[existingIndex] = {
            ...updated[existingIndex],
            cantidad: updated[existingIndex].cantidad + cantidad,
          };
          return updated;
        }

        return [...prev, { producto, cantidad, extras, ingredientesRemovidos: [...new Set(ingredientesRemovidos)] }];
      });
    },
    []
  );

  const removeItem = useCallback((productoId: number, extras: ExtraMenu[], ingredientesRemovidos: string[] = []) => {
    setItems(prev =>
      prev.filter(item => !mismaCombinacion(item, productoId, extras, ingredientesRemovidos))
    );
  }, []);

  const updateQuantity = useCallback(
    (productoId: number, extras: ExtraMenu[], cantidad: number, ingredientesRemovidos: string[] = []) => {
      if (cantidad <= 0) {
        removeItem(productoId, extras, ingredientesRemovidos);
        return;
      }
      setItems(prev =>
        prev.map(item =>
          mismaCombinacion(item, productoId, extras, ingredientesRemovidos)
            ? { ...item, cantidad }
            : item
        )
      );
    },
    [removeItem]
  );

  const clearCart = useCallback(() => setItems([]), []);

  const getItemQuantity = useCallback(
    (productoId: number) => {
      return items
        .filter(item => item.producto.id === productoId)
        .reduce((sum, item) => sum + item.cantidad, 0);
    },
    [items]
  );

  const totalItems = items.reduce((sum, item) => sum + item.cantidad, 0);

  const totalPrice = items.reduce((sum, item) => {
    const extrasTotal = item.extras.reduce(
      (eSum, e) => eSum + e.precio,
      0
    );
    return sum + (item.producto.precio + extrasTotal) * item.cantidad;
  }, 0);

  return (
    <CartContext.Provider
      value={{
        items,
        totalItems,
        totalPrice,
        addItem,
        removeItem,
        updateQuantity,
        clearCart,
        getItemQuantity,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
}
