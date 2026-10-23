/**
 * Estación Burger — Hook de "Mis entregas" (repartidor)
 *
 * Carga GET /delivery/mis-entregas con loading/error/refresh. Expone un flag
 * `noHabilitado` cuando el backend responde 403 (el repartidor aún no está
 * habilitado para repartir) para mostrar un mensaje claro sin romper la UI.
 */

import { useCallback, useEffect, useState } from 'react';
import { ApiError } from '@/shared/api/apiError';
import { deliveryService } from './api';
import type { MisEntregas } from './types';

export interface EstadoMisEntregas {
  datos: MisEntregas | null;
  loading: boolean;
  refreshing: boolean;
  error: string | null;
  noHabilitado: boolean;
  refrescar: () => Promise<void>;
}

export function useMisEntregas(): EstadoMisEntregas {
  const [datos, setDatos] = useState<MisEntregas | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [noHabilitado, setNoHabilitado] = useState(false);

  const cargar = useCallback(async (esRefresh: boolean) => {
    if (esRefresh) setRefreshing(true);
    setError(null);
    setNoHabilitado(false);
    try {
      const respuesta = await deliveryService.misEntregas();
      setDatos(respuesta);
    } catch (e) {
      if (e instanceof ApiError && e.status === 403) {
        setNoHabilitado(true);
      } else {
        setError(e instanceof Error ? e.message : 'No se pudieron cargar tus entregas');
      }
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void cargar(false);
  }, [cargar]);

  const refrescar = useCallback(() => cargar(true), [cargar]);

  return { datos, loading, refreshing, error, noHabilitado, refrescar };
}
