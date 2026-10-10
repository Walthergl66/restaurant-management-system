/**
 * Estación Burger — Generadores de idempotencia para pedidos
 *
 * RF-24/25/41: crear y confirmar un pedido son idempotentes por
 * (codigo + idempotencyKey). La clave se genera UNA vez por checkout
 * y se reutiliza en reintentos: una caída de red no duplica pedidos.
 */

/** Código de pedido del cliente (máx. 40 caracteres según el backend). */
export function generarCodigoPedido(): string {
  const stamp = Date.now().toString(36).toUpperCase();
  const random = Math.random().toString(36).slice(2, 8).toUpperCase();
  return `MOB-${stamp}${random}`.slice(0, 40);
}

/** Clave de idempotencia única (máx. 100 caracteres según el backend). */
export function generarIdempotencyKey(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
}
