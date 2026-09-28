/**
 * Estación Burger — Servicio de Autenticación
 * Compatible con POST /api/v1/auth/login
 */

import api from './api';
import { AuthResponse, Usuario } from '../types';

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
};
