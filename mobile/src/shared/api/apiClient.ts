/**
 * Estación Burger — Cliente HTTP
 *
 * Único boundary de datos de la app (toda la UI consume esto, nunca
 * fetch directamente). Responsabilidades:
 *   - Inyecta el access token (JWT) en cada petición
 *   - Mapea errores ProblemDetail del backend a ApiError
 *   - Maneja 204 (sin contenido) y fallos de red
 */

import { API_BASE_URL } from '@/config/environment';
import { authStorage } from '@/shared/storage/authStorage';
import { ApiError, problemDetailToApiError } from './apiError';

class ApiClient {
  private baseUrl: string;
  private accessToken: string | null = null;
  private refreshPromise: Promise<string | null> | null = null;
  private onSessionExpired?: () => void;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl.replace(/\/$/, '');
  }

  /** Registra un callback que se invoca cuando la sesión expira definitivamente. */
  setOnSessionExpired(callback: () => void) {
    this.onSessionExpired = callback;
  }

  /** Guarda el access token activo (null lo quita). */
  setAccessToken(token: string | null) {
    this.accessToken = token;
  }

  /** Refresca el token de acceso de forma atómica y compartida para evitar condiciones de carrera. */
  private async refreshAccessToken(): Promise<string | null> {
    if (this.refreshPromise) {
      return this.refreshPromise;
    }

    this.refreshPromise = (async () => {
      try {
        const stored = await authStorage.load();
        if (!stored || !stored.refreshToken) {
          this.handleSessionExpired();
          return null;
        }

        const refreshUrl = `${this.baseUrl}/auth/refresh`;
        const response = await fetch(refreshUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken: stored.refreshToken }),
        });

        if (!response.ok) {
          this.handleSessionExpired();
          return null;
        }

        const data = (await response.json()) as {
          accessToken: string;
          refreshToken: string;
          usuario?: (typeof stored)['usuario'];
        };

        this.setAccessToken(data.accessToken);
        await authStorage.save({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          usuario: data.usuario ?? stored.usuario,
        });

        return data.accessToken;
      } catch {
        this.handleSessionExpired();
        return null;
      } finally {
        this.refreshPromise = null;
      }
    })();

    return this.refreshPromise;
  }

  private handleSessionExpired() {
    this.setAccessToken(null);
    void authStorage.clear();
    this.onSessionExpired?.();
  }

  async request<T>(
    endpoint: string,
    options: RequestInit = {},
    isRetry = false
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...((options.headers as Record<string, string>) ?? {}),
    };
    if (this.accessToken) {
      headers.Authorization = `Bearer ${this.accessToken}`;
    }

    let response: Response;
    try {
      response = await fetch(url, { ...options, headers });
    } catch {
      throw new ApiError({
        status: 0,
        message:
          'Sin conexión. Verifica que el backend esté activo y la URL sea correcta.',
      });
    }

    if (!response.ok) {
      if (
        response.status === 401 &&
        !endpoint.startsWith('/auth/') &&
        !isRetry
      ) {
        const nuevoToken = await this.refreshAccessToken();
        if (nuevoToken) {
          return this.request<T>(endpoint, options, true);
        }
      }

      const body = await response.json().catch(() => ({}));
      throw problemDetailToApiError(response.status, body);
    }

    if (response.status === 204) {
      return {} as T;
    }

    return (await response.json()) as T;
  }

  get<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'GET' });
  }

  post<T>(endpoint: string, body?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  }

  put<T>(endpoint: string, body?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  }

  patch<T>(endpoint: string, body?: unknown): Promise<T> {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  }

  delete<T>(endpoint: string): Promise<T> {
    return this.request<T>(endpoint, { method: 'DELETE' });
  }
}

export const apiClient = new ApiClient(API_BASE_URL);
