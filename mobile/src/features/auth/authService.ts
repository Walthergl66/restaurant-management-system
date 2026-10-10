/**
 * Estación Burger — Servicio de autenticación
 * Contrato: POST /api/v1/auth/login | /refresh | /logout, GET /auth/me.
 */

import { apiClient } from '../../core/api/apiClient';
import type {
  AuthResponse,
  LoginRequest,
  RefreshTokenRequest,
  UsuarioInfo,
} from './types';

const ENDPOINTS = {
  login: '/auth/login',
  refresh: '/auth/refresh',
  logout: '/auth/logout',
  me: '/auth/me',
} as const;

export const authService = {
  /** Inicia sesión y activa el access token en el cliente HTTP. */
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const auth = await apiClient.post<AuthResponse>(
      ENDPOINTS.login,
      credentials
    );
    apiClient.setAccessToken(auth.accessToken);
    return auth;
  },

  /** Renueva el access token con el refresh token (rotación de familia). */
  async refresh(refreshToken: string): Promise<AuthResponse> {
    const request: RefreshTokenRequest = { refreshToken };
    const auth = await apiClient.post<AuthResponse>(
      ENDPOINTS.refresh,
      request
    );
    apiClient.setAccessToken(auth.accessToken);
    return auth;
  },

  /** Revoca el refresh token en el backend y limpia el token local. */
  async logout(refreshToken: string): Promise<void> {
    try {
      await apiClient.post(ENDPOINTS.logout, { refreshToken });
    } finally {
      apiClient.setAccessToken(null);
    }
  },

  /** Usuario autenticado actual con sus permisos. */
  async getMe(): Promise<UsuarioInfo> {
    return apiClient.get<UsuarioInfo>(ENDPOINTS.me);
  },
};
