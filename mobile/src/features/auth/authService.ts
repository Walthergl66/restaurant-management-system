/**
 * Estación Burger — Servicio de autenticación
 * Contrato: POST /api/v1/auth/login | /refresh | /logout, GET /auth/me.
 */

import { apiClient } from '../../core/api/apiClient';
import type {
  AuthResponse,
  LoginRequest,
  RefreshTokenRequest,
  RegistroRequest,
  RestablecerPasswordRequest,
  SolicitarRecuperacionRequest,
  UsuarioInfo,
} from './types';

const ENDPOINTS = {
  login: '/auth/login',
  refresh: '/auth/refresh',
  logout: '/auth/logout',
  me: '/auth/me',
  registro: '/auth/registro',
  solicitarRecuperacion: '/auth/solicitar-recuperacion',
  restablecerPassword: '/auth/restablecer-password',
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

  /**
   * Registro público de cliente (RF-45). El backend crea el usuario con
   * rol CLIENTE y devuelve tokens, así que la sesión queda iniciada.
   */
  async registro(datos: RegistroRequest): Promise<AuthResponse> {
    const auth = await apiClient.post<AuthResponse>(
      ENDPOINTS.registro,
      datos
    );
    apiClient.setAccessToken(auth.accessToken);
    return auth;
  },

  /**
   * Solicita el envío de un enlace/código de recuperación. Responde
   * siempre 204 (no revela si el usuario existe).
   */
  async solicitarRecuperacion(username: string): Promise<void> {
    const body: SolicitarRecuperacionRequest = { username };
    await apiClient.post(ENDPOINTS.solicitarRecuperacion, body);
  },

  /** Restablece la contraseña con el token de un solo uso (RF-45). */
  async restablecerPassword(
    token: string,
    nuevaPassword: string
  ): Promise<void> {
    const body: RestablecerPasswordRequest = { token, nuevaPassword };
    await apiClient.post(ENDPOINTS.restablecerPassword, body);
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
