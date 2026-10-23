/**
 * Estación Burger — Flujo de armado de multientrega (delivery)
 *
 * Orquesta la FASE 8 de la ruta de múltiples entregas para el repartidor:
 *   1. Selección de varias asignaciones 'ASIGNADA' sin recorrido en curso.
 *   2. Optimizar: POST /rutas/optimizar -> PropuestaMultientrega. El ORDEN de
 *      paradas lo decide ORS Optimization (VROOM) en el backend; aquí solo se
 *      guarda y respeta tal cual (no se reordena en el cliente).
 *   3. Crear + iniciar: POST /recorridos { tipo:'MULTIPLE', asignacionIds EN EL
 *      ORDEN PROPUESTO } y luego POST /recorridos/{id}/iniciar.
 *
 * Si el backend no pudo optimizar todas las paradas, la llamada falla con
 * ApiError y se expone en `error` (p. ej. 503 = ORS no disponible). No se
 * inventa un orden alternativo ni se permite iniciar sin propuesta válida.
 *
 * La confirmación parada-por-parada y el recálculo son FASE 9.
 */

import { useCallback, useMemo, useState } from 'react';
import { ApiError } from '@/shared/api/apiError';
import { deliveryService } from './api';
import type {
  CriterioOptimizacion,
  EntregaResumen,
  PropuestaMultientrega,
  RecorridoResumen,
} from './types';

/** Mínimo de paradas para que tenga sentido una multientrega. */
export const MIN_PARADAS_MULTIENTREGA = 2;

export type AccionMultientrega = null | 'optimizar' | 'iniciar';

export interface EstadoMultientrega {
  /** asignacionIds seleccionados por el repartidor (orden de selección). */
  seleccion: number[];
  /** Propuesta devuelta por el backend (orden VROOM), o null si no optimizada. */
  propuesta: PropuestaMultientrega | null;
  /** Acción en curso para deshabilitar botones y evitar doble envío. */
  accion: AccionMultientrega;
  /** Mensaje de error legible (optimizar/iniciar), o null. */
  error: string | null;
  /** true cuando hay 2+ seleccionadas (umbral de multientrega). */
  puedeOptimizar: boolean;
  /** Alterna la selección de una asignación (y descarta la propuesta previa). */
  alternar: (asignacionId: number) => void;
  /** Lanza la optimización del orden con el criterio indicado. */
  optimizar: (criterio: CriterioOptimizacion) => Promise<void>;
  /** Crea el recorrido MULTIPLE en el orden propuesto y lo inicia. */
  iniciarRecorrido: () => Promise<RecorridoResumen | null>;
  /** Limpia la propuesta (p. ej. al cambiar la selección). */
  reiniciarPropuesta: () => void;
}

function mensajeError(e: unknown, fallback: string): string {
  if (e instanceof ApiError && e.status === 503) {
    return 'El servicio de rutas no está disponible ahora mismo. Inténtalo de nuevo en unos minutos.';
  }
  return e instanceof Error ? e.message : fallback;
}

/**
 * @param disponibles asignaciones seleccionables (ASIGNADA sin recorrido activo).
 */
export function useMultientrega(disponibles: EntregaResumen[]): EstadoMultientrega {
  const [seleccion, setSeleccion] = useState<number[]>([]);
  const [propuesta, setPropuesta] = useState<PropuestaMultientrega | null>(null);
  const [accion, setAccion] = useState<AccionMultientrega>(null);
  const [error, setError] = useState<string | null>(null);

  const idsDisponibles = useMemo(
    () => new Set(disponibles.map((d) => d.asignacionId)),
    [disponibles]
  );

  const reiniciarPropuesta = useCallback(() => {
    setPropuesta(null);
    setError(null);
  }, []);

  const alternar = useCallback(
    (asignacionId: number) => {
      if (!idsDisponibles.has(asignacionId)) return;
      setSeleccion((prev) =>
        prev.includes(asignacionId)
          ? prev.filter((id) => id !== asignacionId)
          : [...prev, asignacionId]
      );
      // Cambiar la selección invalida la propuesta calculada antes.
      setPropuesta(null);
      setError(null);
    },
    [idsDisponibles]
  );

  const puedeOptimizar = seleccion.length >= MIN_PARADAS_MULTIENTREGA;

  const optimizar = useCallback(
    async (criterio: CriterioOptimizacion) => {
      if (accion || !puedeOptimizar) return;
      setAccion('optimizar');
      setError(null);
      try {
        const resultado = await deliveryService.optimizar(seleccion, criterio);
        setPropuesta(resultado);
      } catch (e) {
        setPropuesta(null);
        setError(mensajeError(e, 'No se pudo optimizar todas las paradas'));
      } finally {
        setAccion(null);
      }
    },
    [accion, puedeOptimizar, seleccion]
  );

  const iniciarRecorrido = useCallback(async (): Promise<RecorridoResumen | null> => {
    if (accion || !propuesta) return null;
    setAccion('iniciar');
    setError(null);
    try {
      // Respetar EXACTAMENTE el orden de VROOM: paradas ya vienen ordenadas.
      const idsOrdenados = [...propuesta.paradas]
        .sort((a, b) => a.orden - b.orden)
        .map((p) => p.asignacionId);

      const creado = await deliveryService.crearRecorrido(
        'MULTIPLE',
        idsOrdenados,
        propuesta.criterio as CriterioOptimizacion
      );
      const iniciado = await deliveryService.iniciarRecorrido(creado.recorridoId);
      return iniciado;
    } catch (e) {
      setError(mensajeError(e, 'No se pudo iniciar el recorrido'));
      return null;
    } finally {
      setAccion(null);
    }
  }, [accion, propuesta]);

  return {
    seleccion,
    propuesta,
    accion,
    error,
    puedeOptimizar,
    alternar,
    optimizar,
    iniciarRecorrido,
    reiniciarPropuesta,
  };
}
