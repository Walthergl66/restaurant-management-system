/**
 * Estación Burger — Hook de ubicación puntual del cliente (checkout A1)
 *
 * A diferencia del hook del repartidor (que mantiene un watch continuo), aquí
 * el cliente solo quiere FIJAR una vez su posición para colocar el marcador en
 * el mapa del checkout. Expone una función `solicitar()` que pide el permiso de
 * primer plano y hace UNA lectura puntual.
 *
 * Si el usuario DENIEGA el permiso: estado claro 'denegado', NO se inventa
 * ubicación; el checkout permite seguir ajustando el marcador manualmente.
 *
 * expo-location se importa AQUÍ; este hook solo se monta dentro del selector de
 * ubicación del checkout DOMICILIO (no en pantallas que no usan mapa), para no
 * cargar el nativo de más.
 */

import { useCallback, useRef, useState } from 'react';
import * as Location from 'expo-location';

export interface Coordenada {
  latitude: number;
  longitude: number;
}

export type EstadoPermiso = 'inicial' | 'cargando' | 'concedido' | 'denegado';

export interface UbicacionActual {
  estado: EstadoPermiso;
  error: string | null;
  /** Pide permiso + una lectura puntual; devuelve la coordenada o null. */
  solicitar: () => Promise<Coordenada | null>;
}

export function useUbicacionActual(): UbicacionActual {
  const [estado, setEstado] = useState<EstadoPermiso>('inicial');
  const [error, setError] = useState<string | null>(null);
  const enCurso = useRef(false);

  const solicitar = useCallback(async (): Promise<Coordenada | null> => {
    if (enCurso.current) return null;
    enCurso.current = true;
    setError(null);
    setEstado('cargando');
    try {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== Location.PermissionStatus.GRANTED) {
        setEstado('denegado');
        return null;
      }
      setEstado('concedido');
      const actual = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.Balanced,
      });
      return {
        latitude: actual.coords.latitude,
        longitude: actual.coords.longitude,
      };
    } catch (e) {
      setError(
        e instanceof Error ? e.message : 'No se pudo obtener la ubicación'
      );
      setEstado('denegado');
      return null;
    } finally {
      enCurso.current = false;
    }
  }, []);

  return { estado, error, solicitar };
}
