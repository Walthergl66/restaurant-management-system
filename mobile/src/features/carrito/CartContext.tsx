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
import type { ExtraMenu, ProductoMenu } from '../menu/types';

interface CartContextType {
  items: CartItem[];
  totalItems: number;
  totalPrice: number;
  addItem: (producto: ProductoMenu, cantidad?: number, extras?: ExtraMenu[]) => void;
  removeItem: (productoId: number, extras: ExtraMenu[]) => void;
  updateQuantity: (
    productoId: number,
    extras: ExtraMenu[],
    cantidad: number
  ) => void;
  clearCart: () => void;
  getItemQuantity: (productoId: number) => number;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

/** Identidad de un item: mismo producto + mismos extras (mismo orden). */
const mismaCombinacion = (
  a: { producto: ProductoMenu; extras: ExtraMenu[] },
  productoId: number,
  extras: ExtraMenu[]
) =>
  a.producto.id === productoId &&
  JSON.stringify(a.extras) === JSON.stringify(extras);

export function CartProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);

  const addItem = useCallback(
    (producto: ProductoMenu, cantidad: number = 1, extras: ExtraMenu[] = []) => {
      setItems(prev => {
        const existingIndex = prev.findIndex(item =>
          mismaCombinacion(item, producto.id, extras)
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

  const removeItem = useCallback((productoId: number, extras: ExtraMenu[]) => {
    setItems(prev =>
      prev.filter(item => !mismaCombinacion(item, productoId, extras))
    );
  }, []);

  const updateQuantity = useCallback(
    (productoId: number, extras: ExtraMenu[], cantidad: number) => {
      if (cantidad <= 0) {
        removeItem(productoId, extras);
        return;
      }
      setItems(prev =>
        prev.map(item =>
          mismaCombinacion(item, productoId, extras)
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
