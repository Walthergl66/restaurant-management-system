/**
 * Estación Burger — Servicio de Autenticación
 */

import api from './api';
import { AuthResponse, Usuario } from '../types';

const TOKEN_KEY = 'estacion_token';
const REFRESH_KEY = 'estacion_refresh';
const USER_KEY = 'estacion_user';

export interface StoredAuth {
  accessToken: string;
  refreshToken: string;
  usuario: Usuario;
}

export const authService = {
  async login(
    username: string,
    password: string
  ): Promise<StoredAuth> {
    const response = await api.post<AuthResponse>('/auth/login', {
      username,
      password,
    });

    const auth: StoredAuth = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      usuario: response.usuario,
    };

    api.setToken(response.accessToken);
    return auth;
  },

  async refresh(refreshToken: string): Promise<StoredAuth> {
    const response = await api.post<AuthResponse>('/auth/refresh', {
      refreshToken,
    });

    const auth: StoredAuth = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      usuario: response.usuario,
    };

    api.setToken(response.accessToken);
    return auth;
  },

  async logout(refreshToken: string): Promise<void> {
    try {
      await api.post('/auth/logout', { refreshToken });
    } finally {
      api.setToken(null);
    }
  },

  async getMe(): Promise<Usuario> {
    return api.get<Usuario>('/auth/me');
  },

  setStoredAuth(auth: StoredAuth) {
    api.setToken(auth.accessToken);
  },

  clearAuth() {
    api.setToken(null);
  },

  getTokenKey: () => TOKEN_KEY,
  getRefreshKey: () => REFRESH_KEY,
  getUserKey: () => USER_KEY,
};
