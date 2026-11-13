/**
 * Estación Burger — Alerta in-app de llegada del repartidor
 *
 * Dispara UNA alerta (vibración corta + callback visual) cuando el
 * seguimiento de un pedido transiciona a ENTREGADO_POR_REPARTIDOR y aún
 * no está confirmado por el cliente. Anti-duplicado: recuerda el último
 * faseLogistica visto por pedido y solo alerta en la TRANSICIÓN.
 *
 * Sonido: Expo SDK 57 no ofrece reproducción de audio sin añadir una
 * dependencia nativa (expo-av/expo-audio). Para no romper Expo Go del
 * cliente ni introducir dependencias, esta fase degrada a vibración +
 * alerta visual garantizadas (sin promesa de sonido).
 */

import { useCallback, useRef } from 'react';
import { Vibration, Platform } from 'react-native';
import type { FaseLogistica, SeguimientoEntrega } from './types';

/** Vibración corta, tolerante a dispositivos sin soporte. */
function vibrarCorto() {
  try {
    if (Platform.OS === 'ios') {
      Vibration.vibrate();
    } else {
      // patrón breve: espera 0ms, vibra 200ms
      Vibration.vibrate([0, 200]);
    }
  } catch {
    // Dispositivo sin vibración: no romper
  }
}

export interface AlertaLlegada {
  /**
   * Procesa el seguimiento recién obtenido de un pedido. Si detecta la
   * transición a "repartidor llegó / pendiente de confirmación",
   * dispara la vibración y llama a `onLlegada` una sola vez.
   */
  procesar: (
    codigo: string,
    seg: SeguimientoEntrega | null,
    onLlegada: () => void
  ) => void;
  /** Olvida el estado recordado de un pedido (p.ej. al cerrar la pantalla). */
  reset: (codigo?: string) => void;
}

export function useAlertaLlegada(): AlertaLlegada {
  // último faseLogistica visto por código de pedido
  const vistoRef = useRef<Map<string, FaseLogistica | null>>(new Map());

  const procesar = useCallback(
    (
      codigo: string,
      seg: SeguimientoEntrega | null,
      onLlegada: () => void
    ) => {
      const fase = seg?.faseLogistica ?? null;
      const previo = vistoRef.current.get(codigo);
      const yaRegistrado = vistoRef.current.has(codigo);
      vistoRef.current.set(codigo, fase);

      // Solo alertar si es una TRANSICIÓN real (teníamos un valor previo
      // distinto) hacia "entregado por repartidor" pendiente de cliente.
      const esLlegadaPendiente =
        seg?.faseLogistica === 'ENTREGADO_POR_REPARTIDOR' &&
        seg?.faseConfirmacion === 'PENDIENTE_CONFIRMACION_CLIENTE';

      if (!esLlegadaPendiente) return;
      // Primera lectura (abrir la app ya en ese estado) no dispara alerta;
      // evita falsos positivos por reconexión / carga inicial.
      if (!yaRegistrado) return;
      if (previo === 'ENTREGADO_POR_REPARTIDOR') return; // ya estaba

      vibrarCorto();
      onLlegada();
    },
    []
  );

  const reset = useCallback((codigo?: string) => {
    if (codigo) vistoRef.current.delete(codigo);
    else vistoRef.current.clear();
  }, []);

  return { procesar, reset };
}
