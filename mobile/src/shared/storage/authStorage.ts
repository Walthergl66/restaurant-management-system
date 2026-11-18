/**
 * Estación Burger — Persistencia de sesión
 *
 * Almacena la sesión (access/refresh token + usuario) en AsyncStorage.
 */

import AsyncStorage from '@react-native-async-storage/async-storage';
import type { StoredAuth } from '@/features/auth';

const KEYS = {
  accessToken: 'estacion_access_token',
  refreshToken: 'estacion_refresh_token',
  usuario: 'estacion_usuario',
} as const;

export const authStorage = {
  /** Sesión guardada o null si no hay (o está corrupta). */
  async load(): Promise<StoredAuth | null> {
    const rows = await AsyncStorage.multiGet([
      KEYS.accessToken,
      KEYS.refreshToken,
      KEYS.usuario,
    ]);
    const [accessToken, refreshToken, usuarioJson] = rows.map(
      ([, value]) => value
    );

    // Sesión incompleta o corrupta: no se confía en datos parciales.
    if (!accessToken || !refreshToken || !usuarioJson) return null;

    try {
      const usuario = JSON.parse(usuarioJson) as StoredAuth['usuario'];
      if (
        !usuario ||
        typeof usuario.id !== 'number' ||
        typeof usuario.username !== 'string' ||
        !Array.isArray(usuario.permisos)
      ) {
        return null;
      }
      return { accessToken, refreshToken, usuario };
    } catch {
      return null;
    }
  },

  async save(auth: StoredAuth): Promise<void> {
    await AsyncStorage.multiSet([
      [KEYS.accessToken, auth.accessToken],
      [KEYS.refreshToken, auth.refreshToken],
      [KEYS.usuario, JSON.stringify(auth.usuario)],
    ]);
  },

  async clear(): Promise<void> {
    await AsyncStorage.multiRemove([
      KEYS.accessToken,
      KEYS.refreshToken,
      KEYS.usuario,
    ]);
  },
};
