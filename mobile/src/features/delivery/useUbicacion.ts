/**
 * Estación Burger — Hook de ubicación del repartidor (delivery)
 *
 * Solicita permiso de ubicación en PRIMER PLANO solo cuando la pantalla del
 * mapa se monta (no hay seguimiento en segundo plano). Hace una lectura puntual
 * y, si se concede, mantiene un watch de bajo ritmo SOLO mientras la pantalla
 * está activa (se limpia al desmontar).
 *
 * Si el usuario DENIEGA el permiso: estado claro 'denegado', no se inventa
 * ubicación y la pantalla puede seguir mostrando la ruta y los datos (el mapa
 * se centra en restaurante + destino sin la posición del repartidor).
 *
 * expo-location se importa AQUÍ y este hook solo se consume desde pantallas del
 * grupo (delivery), para no romper Expo Go de cliente/mesero.
 */

import { useEffect, useRef, useState } from 'react';
import * as Location from 'expo-location';
import type { LatLng } from './polyline';

export type PermisoUbicacion = 'cargando' | 'concedido' | 'denegado';

export interface EstadoUbicacion {
  ubicacion: LatLng | null;
  permiso: PermisoUbicacion;
  error: string | null;
}

export function useUbicacion(): EstadoUbicacion {
  const [ubicacion, setUbicacion] = useState<LatLng | null>(null);
  const [permiso, setPermiso] = useState<PermisoUbicacion>('cargando');
  const [error, setError] = useState<string | null>(null);
  const subscripcion = useRef<Location.LocationSubscription | null>(null);

  useEffect(() => {
    let activo = true;

    (async () => {
      try {
        const { status } = await Location.requestForegroundPermissionsAsync();
        if (!activo) return;

        if (status !== Location.PermissionStatus.GRANTED) {
          setPermiso('denegado');
          return;
        }

        setPermiso('concedido');

        // Lectura puntual inicial (rápida).
        const actual = await Location.getCurrentPositionAsync({
          accuracy: Location.Accuracy.Balanced,
        });
        if (!activo) return;
        setUbicacion({
          latitude: actual.coords.latitude,
          longitude: actual.coords.longitude,
        });

        // Watch de bajo ritmo solo mientras la pantalla está activa.
        subscripcion.current = await Location.watchPositionAsync(
          {
            accuracy: Location.Accuracy.Balanced,
            timeInterval: 10000,
            distanceInterval: 25,
          },
          (posicion) => {
            if (!activo) return;
            setUbicacion({
              latitude: posicion.coords.latitude,
              longitude: posicion.coords.longitude,
            });
          }
        );
      } catch (e) {
        if (!activo) return;
        setError(e instanceof Error ? e.message : 'No se pudo obtener la ubicación');
      }
    })();

    return () => {
      activo = false;
      subscripcion.current?.remove();
      subscripcion.current = null;
    };
  }, []);

  return { ubicacion, permiso, error };
}
