import { useCallback, useRef } from 'react';
import { useFocusEffect } from 'expo-router';

/**
 * Ejecuta `cargar` al enfocar la pantalla y luego cada `intervaloMs` mientras siga enfocada
 * (se detiene al salir o pasar a otra pestaña, para no gastar batería ni datos).
 * Los errores de `cargar` los gestiona quien llama; aquí no interrumpen el sondeo.
 */
export function useSondeo(cargar: () => void | Promise<void>, intervaloMs: number): void {
  const cargarRef = useRef(cargar);
  cargarRef.current = cargar;

  useFocusEffect(
    useCallback(() => {
      const ejecutar = () => {
        void Promise.resolve(cargarRef.current()).catch(() => undefined);
      };
      ejecutar();
      const id = setInterval(ejecutar, intervaloMs);
      return () => clearInterval(id);
    }, [intervaloMs]),
  );
}
