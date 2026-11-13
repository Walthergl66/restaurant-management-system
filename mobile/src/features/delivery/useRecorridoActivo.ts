/**
 * Estación Burger — Flujo de un recorrido EN CURSO (delivery, FASE 9)
 *
 * Encapsula la operación parada-por-parada de un recorrido ya iniciado:
 *   - Carga del RecorridoResumen (desde GET /mis-entregas, filtrado por id).
 *   - Marcar una parada ENTREGADA con Idempotency-Key POR PARADA (una vez por
 *     parada y reutilizado en reintentos del mismo intento). IDEMPOTENTE.
 *   - Registrar incidencia (parada FALLIDA) con motivo.
 *   - Recalcular la ruta de las paradas PENDIENTES desde la ubicación actual
 *     (manual, no automático). Excluye entregadas/fallidas (lo hace el backend).
 *   - Finalizar el recorrido (solo cuando TODAS las paradas están resueltas).
 *
 * CONEXIÓN / OFFLINE: si una llamada falla con ApiError status 0 (sin conexión)
 * se marca `sinConexion` y NO se borran los datos de ruta ya cargados. La
 * entrega que falle por red se ENCOLA (colaOffline) con su mismo Idempotency-Key
 * y se reintenta al recuperar foco/conexión; como el backend es idempotente,
 * reintentar no duplica. La parada queda en estado 'pendiente de sincronizar'
 * (claramente distinto de 'entregado confirmado') hasta tener respuesta válida.
 *
 * No marca entregas automáticamente por proximidad GPS: la confirmación es
 * siempre manual (el repartidor pulsa el botón).
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '@/shared/api/apiError';
import { deliveryService } from './api';
import { colaEntregas } from './colaOffline';
import type { RecorridoResumen } from './types';
import { nuevoUuid } from './uuid';

export type AccionRecorrido = null | 'recalcular' | 'finalizar';

/** Estado por parada mientras se opera (para deshabilitar y mostrar progreso). */
export type EstadoParadaUi =
  | 'idle'
  | 'enviando'
  | 'pendiente-sync' // encolada offline, aún no confirmada por el servidor
  | 'incidencia';

export interface EstadoRecorridoActivo {
  recorrido: RecorridoResumen | null;
  cargando: boolean;
  error: string | null;
  sinConexion: boolean;
  /** paradaId -> estado de UI de su acción en curso. */
  estadoPorParada: Record<number, EstadoParadaUi>;
  accion: AccionRecorrido;
  accionError: string | null;
  /** Geometría recalculada desde la ubicación actual (sobrescribe el dibujo). */
  geometriaRecalculada: string | null;
  /** true cuando todas las paradas están resueltas (ENTREGADA/FALLIDA). */
  todasResueltas: boolean;
  refrescar: () => Promise<void>;
  marcarEntregada: (paradaId: number) => Promise<void>;
  registrarIncidencia: (paradaId: number, motivo: string) => Promise<void>;
  recalcular: (lat: number, lng: number) => Promise<void>;
  finalizar: () => Promise<boolean>;
  /** Reintenta sincronizar las entregas encoladas (al volver foco/conexión). */
  sincronizarPendientes: () => Promise<void>;
}

function esSinConexion(e: unknown): boolean {
  return e instanceof ApiError && e.status === 0;
}

function mensajeError(e: unknown, fallback: string): string {
  return e instanceof Error ? e.message : fallback;
}

export function useRecorridoActivo(recorridoId: number): EstadoRecorridoActivo {
  const [recorrido, setRecorrido] = useState<RecorridoResumen | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [sinConexion, setSinConexion] = useState(false);
  const [estadoPorParada, setEstadoPorParada] = useState<Record<number, EstadoParadaUi>>({});
  const [accion, setAccion] = useState<AccionRecorrido>(null);
  const [accionError, setAccionError] = useState<string | null>(null);
  const [geometriaRecalculada, setGeometriaRecalculada] = useState<string | null>(null);

  // Idempotency-Key POR PARADA: generado UNA vez y reutilizado en reintentos.
  const keysPorParada = useRef<Map<number, string>>(new Map());

  const ponerEstadoParada = useCallback((paradaId: number, estado: EstadoParadaUi) => {
    setEstadoPorParada((prev) => ({ ...prev, [paradaId]: estado }));
  }, []);

  const keyDeParada = useCallback(async (paradaId: number): Promise<string> => {
    const enMemoria = keysPorParada.current.get(paradaId);
    if (enMemoria) return enMemoria;
    // Reusa la key ya encolada (si quedó de un intento offline anterior).
    const encolada = await colaEntregas.keyDe(paradaId);
    const key = encolada ?? nuevoUuid();
    keysPorParada.current.set(paradaId, key);
    return key;
  }, []);

  const cargar = useCallback(async () => {
    setError(null);
    try {
      const datos = await deliveryService.misEntregas();
      const actual = datos.recorridos.find((r) => r.recorridoId === recorridoId) ?? null;
      setRecorrido(actual);
      setSinConexion(false);
      if (!actual) {
        setError('El recorrido ya no está activo.');
      }
    } catch (e) {
      if (esSinConexion(e)) {
        // Conserva los datos ya cargados; solo marca sin conexión.
        setSinConexion(true);
      } else if (e instanceof ApiError && e.status === 403) {
        setError('No tienes acceso a este recorrido.');
      } else {
        setError(mensajeError(e, 'No se pudo cargar el recorrido'));
      }
    } finally {
      setCargando(false);
    }
  }, [recorridoId]);

  useEffect(() => {
    void cargar();
  }, [cargar]);

  const marcarEntregada = useCallback(
    async (paradaId: number) => {
      if (estadoPorParada[paradaId] === 'enviando') return;
      // Evita re-confirmar una parada ya ENTREGADA.
      const yaEntregada = recorrido?.paradas.find((p) => p.paradaId === paradaId)?.estado === 'ENTREGADA';
      if (yaEntregada) return;

      ponerEstadoParada(paradaId, 'enviando');
      setAccionError(null);
      const key = await keyDeParada(paradaId);
      try {
        await deliveryService.marcarEntregada(paradaId, key);
        // Confirmada por el servidor: limpia cola y key, refresca estado.
        await colaEntregas.quitar(paradaId);
        keysPorParada.current.delete(paradaId);
        ponerEstadoParada(paradaId, 'idle');
        setSinConexion(false);
        await cargar();
      } catch (e) {
        if (esSinConexion(e)) {
          // Encola con la MISMA key y marca 'pendiente de sincronizar'.
          await colaEntregas.encolar(paradaId, key);
          setSinConexion(true);
          ponerEstadoParada(paradaId, 'pendiente-sync');
        } else {
          ponerEstadoParada(paradaId, 'idle');
          setAccionError(mensajeError(e, 'No se pudo marcar como entregado'));
        }
      }
    },
    [estadoPorParada, recorrido, ponerEstadoParada, keyDeParada, cargar]
  );

  const registrarIncidencia = useCallback(
    async (paradaId: number, motivo: string) => {
      if (estadoPorParada[paradaId] === 'enviando') return;
      ponerEstadoParada(paradaId, 'enviando');
      setAccionError(null);
      try {
        await deliveryService.registrarIncidencia(paradaId, motivo);
        ponerEstadoParada(paradaId, 'idle');
        setSinConexion(false);
        await cargar();
      } catch (e) {
        ponerEstadoParada(paradaId, 'idle');
        if (esSinConexion(e)) {
          setSinConexion(true);
          setAccionError('Sin conexión: no se pudo registrar la incidencia. Reintenta cuando vuelva la red.');
        } else {
          setAccionError(mensajeError(e, 'No se pudo registrar la incidencia'));
        }
      }
    },
    [estadoPorParada, ponerEstadoParada, cargar]
  );

  const recalcular = useCallback(
    async (lat: number, lng: number) => {
      if (accion) return;
      setAccion('recalcular');
      setAccionError(null);
      try {
        const ruta = await deliveryService.recalcular(recorridoId, lat, lng);
        setGeometriaRecalculada(ruta.geometria);
        setSinConexion(false);
      } catch (e) {
        if (esSinConexion(e)) {
          setSinConexion(true);
          setAccionError('Sin conexión: no se pudo recalcular la ruta.');
        } else {
          setAccionError(mensajeError(e, 'No se pudo recalcular la ruta'));
        }
      } finally {
        setAccion(null);
      }
    },
    [accion, recorridoId]
  );

  const finalizar = useCallback(async (): Promise<boolean> => {
    if (accion) return false;
    setAccion('finalizar');
    setAccionError(null);
    try {
      await deliveryService.finalizarRecorrido(recorridoId);
      setSinConexion(false);
      return true;
    } catch (e) {
      if (esSinConexion(e)) {
        setSinConexion(true);
        setAccionError('Sin conexión: no se pudo finalizar el recorrido.');
      } else {
        setAccionError(mensajeError(e, 'No se pudo finalizar el recorrido'));
      }
      return false;
    } finally {
      setAccion(null);
    }
  }, [accion, recorridoId]);

  const sincronizarPendientes = useCallback(async () => {
    const pendientes = await colaEntregas.listar();
    if (pendientes.length === 0) return;
    let algunaOk = false;
    for (const item of pendientes) {
      try {
        await deliveryService.marcarEntregada(item.paradaId, item.idempotencyKey);
        await colaEntregas.quitar(item.paradaId);
        keysPorParada.current.delete(item.paradaId);
        ponerEstadoParada(item.paradaId, 'idle');
        algunaOk = true;
      } catch (e) {
        if (esSinConexion(e)) {
          // Sigue sin conexión: deja el resto en cola y corta.
          setSinConexion(true);
          break;
        }
        // Error no-red (p. ej. 422 parada ya resuelta): la quitamos para no
        // reintentar indefinidamente; el refresco reflejará el estado real.
        await colaEntregas.quitar(item.paradaId);
        keysPorParada.current.delete(item.paradaId);
        ponerEstadoParada(item.paradaId, 'idle');
        algunaOk = true;
      }
    }
    if (algunaOk) {
      setSinConexion(false);
      await cargar();
    }
  }, [ponerEstadoParada, cargar]);

  const paradas = recorrido?.paradas ?? [];
  const todasResueltas =
    paradas.length > 0 && paradas.every((p) => p.estado === 'ENTREGADA' || p.estado === 'FALLIDA');

  return {
    recorrido,
    cargando,
    error,
    sinConexion,
    estadoPorParada,
    accion,
    accionError,
    geometriaRecalculada,
    todasResueltas,
    refrescar: cargar,
    marcarEntregada,
    registrarIncidencia,
    recalcular,
    finalizar,
    sincronizarPendientes,
  };
}
