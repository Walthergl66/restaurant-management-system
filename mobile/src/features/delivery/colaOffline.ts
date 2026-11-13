/**
 * Estación Burger — Cola offline idempotente para "marcar entregada" (delivery)
 *
 * Cuando "marcar entregado" falla por SIN CONEXIÓN (ApiError status 0), la
 * acción {paradaId, idempotencyKey} se encola en AsyncStorage y se reintenta
 * al recuperar la conexión (o al volver el foco a la pantalla). El backend es
 * idempotente por parada, así que reintentar con el MISMO Idempotency-Key no
 * duplica la entrega.
 *
 * Se reutiliza @react-native-async-storage/async-storage (ya presente en el
 * proyecto). No se añade @react-native-community/netinfo: la conexión se infiere
 * del resultado de las requests (status 0) más el reintento manual/por foco.
 */

import AsyncStorage from '@react-native-async-storage/async-storage';

/** Una entrega pendiente de sincronizar con el servidor. */
export interface EntregaEncolada {
  paradaId: number;
  idempotencyKey: string;
  /** Epoch ms del primer encolado (para orden/diagnóstico). */
  encoladaAt: number;
}

const STORAGE_KEY = 'delivery:cola-entregas';

async function leerTodo(): Promise<EntregaEncolada[]> {
  try {
    const crudo = await AsyncStorage.getItem(STORAGE_KEY);
    if (!crudo) return [];
    const parsed = JSON.parse(crudo) as unknown;
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (e): e is EntregaEncolada =>
        typeof e === 'object' &&
        e !== null &&
        typeof (e as EntregaEncolada).paradaId === 'number' &&
        typeof (e as EntregaEncolada).idempotencyKey === 'string'
    );
  } catch {
    return [];
  }
}

async function escribirTodo(items: EntregaEncolada[]): Promise<void> {
  await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(items));
}

export const colaEntregas = {
  /** Todas las entregas pendientes de sincronizar. */
  listar(): Promise<EntregaEncolada[]> {
    return leerTodo();
  },

  /**
   * Encola (o actualiza) una entrega pendiente. Idempotente por paradaId:
   * si ya existe una entrada para esa parada, conserva su Idempotency-Key
   * original (reintentar con la misma key no duplica en el backend).
   */
  async encolar(paradaId: number, idempotencyKey: string): Promise<void> {
    const items = await leerTodo();
    const existente = items.find((e) => e.paradaId === paradaId);
    if (existente) return; // conserva la key original del primer intento
    items.push({ paradaId, idempotencyKey, encoladaAt: Date.now() });
    await escribirTodo(items);
  },

  /** Idempotency-Key ya encontrado para una parada, o undefined. */
  async keyDe(paradaId: number): Promise<string | undefined> {
    const items = await leerTodo();
    return items.find((e) => e.paradaId === paradaId)?.idempotencyKey;
  },

  /** Quita una parada de la cola (tras sincronizar con éxito). */
  async quitar(paradaId: number): Promise<void> {
    const items = await leerTodo();
    const restantes = items.filter((e) => e.paradaId !== paradaId);
    if (restantes.length !== items.length) {
      await escribirTodo(restantes);
    }
  },

  /** true si la parada tiene una entrega pendiente de sincronizar. */
  async tienePendiente(paradaId: number): Promise<boolean> {
    const items = await leerTodo();
    return items.some((e) => e.paradaId === paradaId);
  },
};
