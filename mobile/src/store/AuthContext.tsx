/**
 * Estación Burger — Contexto de Autenticación
 */

import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  useEffect,
  ReactNode,
} from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { authService, StoredAuth } from '../services/auth';
import api from '../services/api';
import { Usuario } from '../types';

interface AuthContextType {
  usuario: Usuario | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshSession: () => Promise<boolean>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStoredAuth();
  }, []);

  const loadStoredAuth = async () => {
    try {
      const [token, refreshToken, userStr] = await Promise.all([
        AsyncStorage.getItem('estacion_token'),
        AsyncStorage.getItem('estacion_refresh'),
        AsyncStorage.getItem('estacion_user'),
      ]);

      if (token && userStr) {
        api.setToken(token);
        setUsuario(JSON.parse(userStr));
      }
    } catch {
      // Silently fail
    } finally {
      setLoading(false);
    }
  };

  const login = useCallback(
    async (username: string, password: string) => {
      const auth = await authService.login(username, password);
      await AsyncStorage.multiSet([
        ['estacion_token', auth.accessToken],
        ['estacion_refresh', auth.refreshToken],
        ['estacion_user', JSON.stringify(auth.usuario)],
      ]);
      api.setToken(auth.accessToken);
      setUsuario(auth.usuario);
    },
    []
  );

  const logout = useCallback(async () => {
    try {
      const refreshToken = await AsyncStorage.getItem('estacion_refresh');
      if (refreshToken) {
        await authService.logout(refreshToken);
      }
    } finally {
      await AsyncStorage.multiRemove([
        'estacion_token',
        'estacion_refresh',
        'estacion_user',
      ]);
      api.setToken(null);
      setUsuario(null);
    }
  }, []);

  const refreshSession = useCallback(async (): Promise<boolean> => {
    try {
      const refreshToken = await AsyncStorage.getItem('estacion_refresh');
      if (!refreshToken) return false;

      const auth = await authService.refresh(refreshToken);
      await AsyncStorage.multiSet([
        ['estacion_token', auth.accessToken],
        ['estacion_refresh', auth.refreshToken],
        ['estacion_user', JSON.stringify(auth.usuario)],
      ]);
      api.setToken(auth.accessToken);
      setUsuario(auth.usuario);
      return true;
    } catch {
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
