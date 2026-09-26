/**
 * Estación Burger — Contexto de Carrito
 */

import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  ReactNode,
} from 'react';
import { CartItem, Producto, Extra } from '../types';

interface CartContextType {
  items: CartItem[];
  totalItems: number;
  totalPrice: number;
  addItem: (producto: Producto, cantidad?: number, extras?: Extra[]) => void;
  removeItem: (productoId: number, extras: Extra[]) => void;
  updateQuantity: (
    productoId: number,
    extras: Extra[],
    cantidad: number
  ) => void;
  clearCart: () => void;
  getItemQuantity: (productoId: number) => number;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);

  const addItem = useCallback(
    (producto: Producto, cantidad: number = 1, extras: Extra[] = []) => {
      setItems(prev => {
        const existingIndex = prev.findIndex(
          item =>
            item.producto.id === producto.id &&
            JSON.stringify(item.extras) === JSON.stringify(extras)
        );

        if (existingIndex >= 0) {
          const updated = [...prev];
          updated[existingIndex] = {
            ...updated[existingIndex],
            cantidad: updated[existingIndex].cantidad + cantidad,
          };
          return updated;
        }

        return [...prev, { producto, cantidad, extras }];
      });
    },
    []
  );

  const removeItem = useCallback(
    (productoId: number, extras: Extra[]) => {
      setItems(prev =>
        prev.filter(
          item =>
            !(
              item.producto.id === productoId &&
              JSON.stringify(item.extras) === JSON.stringify(extras)
            )
        )
      );
    },
    []
  );

  const updateQuantity = useCallback(
    (productoId: number, extras: Extra[], cantidad: number) => {
      if (cantidad <= 0) {
        removeItem(productoId, extras);
        return;
      }
      setItems(prev =>
        prev.map(item =>
          item.producto.id === productoId &&
          JSON.stringify(item.extras) === JSON.stringify(extras)
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
      (eSum, e) => eSum + parseFloat(e.precio),
      0
    );
    return (
      sum + (parseFloat(item.producto.precio) + extrasTotal) * item.cantidad
    );
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
