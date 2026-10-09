/**
 * Estación Burger — Contexto de autenticación
 *
 * Mantiene la sesión en memoria + AsyncStorage y expone
 * login/logout/refresh a toda la app vía useAuth().
 */

import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  ReactNode,
} from 'react';
import { authService } from './authService';
import { authStorage } from '../../core/storage/authStorage';
import { apiClient } from '../../core/api/apiClient';
import type { UsuarioInfo } from './types';

interface AuthContextType {
  usuario: UsuarioInfo | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioInfo | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    void restoreSession();
  }, []);

  /** Restaura la sesión guardada al arrancar la app. */
  const restoreSession = async () => {
    try {
      const stored = await authStorage.load();
      if (stored) {
        apiClient.setAccessToken(stored.accessToken);
        setUsuario(stored.usuario);
      }
    } catch {
      // Sesión corrupta: arrancar sin sesión
    } finally {
      setLoading(false);
    }
  };

  const login = useCallback(async (username: string, password: string) => {
    const auth = await authService.login({ username, password });
    await authStorage.save({
      accessToken: auth.accessToken,
      refreshToken: auth.refreshToken,
      usuario: auth.usuario,
    });
    setUsuario(auth.usuario);
  }, []);

  const logout = useCallback(async () => {
    const stored = await authStorage.load();
    if (stored) {
      await authService.logout(stored.refreshToken);
    }
    await authStorage.clear();
    setUsuario(null);
  }, []);

  const refreshSession = useCallback(async (): Promise<boolean> => {
    const stored = await authStorage.load();
    if (!stored) return false;

    try {
      const auth = await authService.refresh(stored.refreshToken);
      await authStorage.save({
        accessToken: auth.accessToken,
        refreshToken: auth.refreshToken,
        usuario: auth.usuario,
      });
      setUsuario(auth.usuario);
      return true;
    } catch {
      await authStorage.clear();
      setUsuario(null);
      return false;
    }
  }, []);

  return (
    <AuthContext.Provider
      value={{
        usuario,
        loading,
        isAuthenticated: !!usuario,
        login,
        logout,
        refreshSession,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
