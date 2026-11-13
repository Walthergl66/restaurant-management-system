/**
 * Estación Burger — Generador de UUID (delivery)
 *
 * Para el header Idempotency-Key de "marcar entregada". Usa crypto.randomUUID
 * si existe en el runtime; si no, un fallback v4 simple basado en Math.random
 * (suficiente para idempotencia de una operación puntual). No añadimos una
 * dependencia solo para esto.
 */

export function nuevoUuid(): string {
  const c = (globalThis as { crypto?: { randomUUID?: () => string } }).crypto;
  if (c?.randomUUID) {
    return c.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (ch) => {
    const r = (Math.random() * 16) | 0;
    const v = ch === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}
