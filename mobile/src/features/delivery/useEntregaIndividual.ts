/**
 * Estación Burger — Flujo de entrega individual (delivery)
 *
 * Orquesta el flujo real cableado al backend para UNA asignación:
 *   1. Carga la ruta individual (POST /rutas/individual) -> geometría + totales.
 *   2. Iniciar reparto: si no hay recorrido, crea uno INDIVIDUAL y lo inicia.
 *   3. Marcar entregado: POST /paradas/{id}/entregada con Idempotency-Key.
 *   4. Finalizar recorrido: POST /recorridos/{id}/finalizar.
 *
 * Estados de carga por acción para deshabilitar botones y evitar doble envío.
 * El 503 (rutas no disponibles) se expone como `rutaNoDisponible` para mostrar
 * un mensaje claro sin romper la pantalla.
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError } from '@/shared/api/apiError';
import { deliveryService } from './api';
import type { EntregaResumen, MisEntregas, RecorridoResumen, RutaCalculada } from './types';
import type { LatLng } from './polyline';
import { nuevoUuid } from './uuid';

export type AccionCargando = null | 'iniciar' | 'entregar' | 'finalizar';

export interface EstadoEntregaIndividual {
  etapa: 'recogida' | 'entrega';
  ruta: RutaCalculada | null;
  rutaCargando: boolean;
  rutaNoDisponible: boolean;
  rutaError: string | null;
  recorridoId: number | null;
  paradaId: number | null;
  paradaEntregada: boolean;
  recorridoEnCurso: boolean;
  recorridoFinalizado: boolean;
  accion: AccionCargando;
  accionError: string | null;
  iniciarReparto: () => Promise<void>;
  marcarEntregado: () => Promise<void>;
  finalizarRecorrido: () => Promise<void>;
}

/** Resuelve estado inicial (recorrido/parada) desde mis-entregas si ya existe. */
function recorridoDeAsignacion(
  datos: MisEntregas | null,
  asignacionId: number
): RecorridoResumen | undefined {
  if (!datos) return undefined;
  return datos.recorridos.find((r) =>
    r.paradas.some((p) => p.asignacionId === asignacionId)
  );
}

function mensajeError(e: unknown, fallback: string): string {
  return e instanceof Error ? e.message : fallback;
}

function distanciaMetros(a: LatLng, b: LatLng): number {
  const radio = 6371000;
  const dLat = ((b.latitude - a.latitude) * Math.PI) / 180;
  const dLng = ((b.longitude - a.longitude) * Math.PI) / 180;
  const h =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((a.latitude * Math.PI) / 180) *
      Math.cos((b.latitude * Math.PI) / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  return 2 * radio * Math.asin(Math.sqrt(h));
}

export function useEntregaIndividual(
  asignacionId: number,
  entrega: EntregaResumen | undefined,
  datos: MisEntregas | null,
  ubicacionRepartidor: LatLng | null = null
): EstadoEntregaIndividual {
  const [etapa, setEtapa] = useState<'recogida' | 'entrega'>('recogida');
  const [ruta, setRuta] = useState<RutaCalculada | null>(null);
  const [rutaCargando, setRutaCargando] = useState(true);
  const [rutaNoDisponible, setRutaNoDisponible] = useState(false);
  const [rutaError, setRutaError] = useState<string | null>(null);

  const [recorridoId, setRecorridoId] = useState<number | null>(null);
  const [paradaId, setParadaId] = useState<number | null>(null);
  const [paradaEntregada, setParadaEntregada] = useState(false);
  const [recorridoEnCurso, setRecorridoEnCurso] = useState(false);
  const [recorridoFinalizado, setRecorridoFinalizado] = useState(false);

  const [accion, setAccion] = useState<AccionCargando>(null);
  const [accionError, setAccionError] = useState<string | null>(null);

  // Idempotency-Key reutilizado entre reintentos del MISMO intento de entrega.
  const idempotencyKey = useRef<string | null>(null);
  const recalculandoRef = useRef(false);
  const ultimaUbicacionRef = useRef<LatLng | null>(null);
  const requestIdRef = useRef(0);
  // Estado inicial a partir de un recorrido ya existente para esta asignación.
  useEffect(() => {
    const existente = recorridoDeAsignacion(datos, asignacionId);
    if (!existente) return;
    const parada = existente.paradas.find((p) => p.asignacionId === asignacionId);
    setRecorridoId(existente.recorridoId);
    setParadaId(parada?.paradaId ?? null);
    setRecorridoEnCurso(existente.estado === 'EN_CURSO' || existente.estado === 'PLANIFICADO');
    setParadaEntregada(parada?.estado === 'ENTREGADA');
    setRecorridoFinalizado(
      existente.estado === 'FINALIZADO' || existente.estado === 'CANCELADO'
    );
    setEtapa('entrega');
  }, [datos, asignacionId]);

  // Carga inicial: la ruta activa depende del estado confirmado por backend.
  useEffect(() => {
    if (!entrega) return;
    let activo = true;
    (async () => {
      setRutaCargando(true);
      setRutaNoDisponible(false);
      setRutaError(null);
      const existente = recorridoDeAsignacion(datos, asignacionId);
      if (!existente) {
        try {
          const historial = await deliveryService.historial();
          const finalizado = historial.find((r) =>
            r.paradas.some((p) => p.asignacionId === asignacionId)
          );
          if (activo && finalizado) {
            aplicarRecorridoExistente(finalizado);
          }
        } catch {
          // Si falla el historial, se mantiene la etapa por defecto.
        }
      }
      const estadoActual = existente ?? recorridoDeAsignacion(datos, asignacionId);
      const esFinalizado =
        estadoActual?.estado === 'FINALIZADO' || estadoActual?.estado === 'CANCELADO';
      if (esFinalizado) {
        try {
          const calculada = await deliveryService.rutaIndividual(asignacionId);
          if (activo) setRuta(calculada);
        } catch (e) {
          if (!activo) return;
          if (e instanceof ApiError && e.status === 503) setRutaNoDisponible(true);
          else setRutaError(mensajeError(e, 'No se pudo cargar la ruta'));
        } finally {
          if (activo) setRutaCargando(false);
        }
        return;
      }
      if (activo) {
        setRuta(null);
        setRutaCargando(false);
      }
    })();
    return () => {
      activo = false;
    };
  }, [asignacionId, entrega, datos]);

  const recalcularRutaDinamica = useCallback(async () => {
    if (!ubicacionRepartidor) {
      setRutaError('Se necesita GPS para calcular la ruta.');
      return;
    }
    if (etapa === 'entrega' && (!recorridoId || !recorridoEnCurso)) return;
    const ultima = ultimaUbicacionRef.current;
    if (ultima && distanciaMetros(ultima, ubicacionRepartidor) < 30) return;
    if (recalculandoRef.current) return;
    recalculandoRef.current = true;
    const requestId = ++requestIdRef.current;
    try {
      const nueva =
        etapa === 'recogida'
          ? await deliveryService.rutaRecogida(
              ubicacionRepartidor.latitude,
              ubicacionRepartidor.longitude
            )
          : await deliveryService.recalcular(
              recorridoId as number,
              ubicacionRepartidor.latitude,
              ubicacionRepartidor.longitude
            );
      if (requestId !== requestIdRef.current) return;
      setRuta(nueva);
      setRutaError(null);
      ultimaUbicacionRef.current = ubicacionRepartidor;
    } catch {
      if (requestId !== requestIdRef.current) return;
      setRutaError('No se pudo actualizar la ruta; se mantiene la última válida.');
    } finally {
      recalculandoRef.current = false;
    }
  }, [recorridoId, recorridoEnCurso, ubicacionRepartidor]);

  useEffect(() => {
    void recalcularRutaDinamica();
  }, [recalcularRutaDinamica]);

  const aplicarRecorridoExistente = useCallback((existente: RecorridoResumen) => {
    const parada = existente.paradas.find((p) => p.asignacionId === asignacionId);
    setRecorridoId(existente.recorridoId);
    setParadaId(parada?.paradaId ?? null);
    setRecorridoEnCurso(existente.estado === 'EN_CURSO' || existente.estado === 'PLANIFICADO');
    setParadaEntregada(parada?.estado === 'ENTREGADA');
    const finalizado = existente.estado === 'FINALIZADO' || existente.estado === 'CANCELADO';
    setRecorridoFinalizado(finalizado);
    setAccionError(finalizado ? 'El recorrido ya fue finalizado.' : null);
    setEtapa('entrega');
  }, [asignacionId]);

  const buscarRecorridoExistente = useCallback(async (): Promise<RecorridoResumen | undefined> => {
    try {
      const actualizados = await deliveryService.misEntregas();
      const existente = actualizados.recorridos.find((r) =>
        r.paradas.some((p) => p.asignacionId === asignacionId)
      );
      if (existente) return existente;
    } catch {
      // Si falla, se intenta con el historial finalizado.
    }
    try {
      const historial = await deliveryService.historial();
      return historial.find((r) => r.paradas.some((p) => p.asignacionId === asignacionId));
    } catch {
      return undefined;
    }
  }, [asignacionId]);

  const iniciarReparto = useCallback(async () => {
    if (accion) return;
    setAccion('iniciar');
    setAccionError(null);
    try {
      let idRecorrido = recorridoId;
      let idParada = paradaId;

      if (idRecorrido == null) {
        const existente = await buscarRecorridoExistente();
        if (existente) {
          aplicarRecorridoExistente(existente);
          return;
        }
        try {
          const creado = await deliveryService.crearRecorrido(
            'INDIVIDUAL',
            [asignacionId],
            'DURACION'
          );
          idRecorrido = creado.recorridoId;
          idParada = creado.paradas.find((p) => p.asignacionId === asignacionId)?.paradaId ?? null;
          setRecorridoId(idRecorrido);
          setParadaId(idParada);
          setRecorridoFinalizado(false);
          setEtapa('entrega');
        } catch (e) {
          if (e instanceof ApiError && e.status === 409) {
            const existente = await buscarRecorridoExistente();
            if (existente) {
              aplicarRecorridoExistente(existente);
              setAccionError('La asignación ya pertenece a un recorrido.');
              return;
            }
          }
          throw e;
        }
      }

      const iniciado = await deliveryService.iniciarRecorrido(idRecorrido);
      setRecorridoEnCurso(iniciado.estado === 'EN_CURSO');
      const parada = iniciado.paradas.find((p) => p.asignacionId === asignacionId);
      if (parada) setParadaId(parada.paradaId);
      setRecorridoFinalizado(false);
      setEtapa('entrega');
    } catch (e) {
      setAccionError(mensajeError(e, 'No se pudo iniciar el reparto'));
    } finally {
      setAccion(null);
    }
  }, [accion, recorridoId, paradaId, asignacionId, buscarRecorridoExistente, aplicarRecorridoExistente]);

  const marcarEntregado = useCallback(async () => {
    if (accion || paradaId == null || paradaEntregada) return;
    setAccion('entregar');
    setAccionError(null);
    // Un uuid por intento; se reutiliza en reintentos del mismo tap.
    if (!idempotencyKey.current) idempotencyKey.current = nuevoUuid();
    try {
      const respuesta = await deliveryService.marcarEntregada(paradaId, idempotencyKey.current);
      setParadaEntregada(respuesta.estado === 'ENTREGADA');
      idempotencyKey.current = null;
    } catch (e) {
      setAccionError(mensajeError(e, 'No se pudo marcar como entregado'));
    } finally {
      setAccion(null);
    }
  }, [accion, paradaId, paradaEntregada]);

  const finalizarRecorrido = useCallback(async () => {
    if (accion || recorridoId == null || !paradaEntregada) return;
    setAccion('finalizar');
    setAccionError(null);
    try {
      await deliveryService.finalizarRecorrido(recorridoId);
    } catch (e) {
      setAccionError(mensajeError(e, 'No se pudo finalizar el recorrido'));
      throw e;
    } finally {
      setAccion(null);
    }
  }, [accion, recorridoId, paradaEntregada]);

  return {
    etapa,
    ruta,
    rutaCargando,
    rutaNoDisponible,
    rutaError,
    recorridoId,
    paradaId,
    paradaEntregada,
    recorridoEnCurso,
    recorridoFinalizado,
    accion,
    accionError,
    iniciarReparto,
    marcarEntregado,
    finalizarRecorrido,
  };
}
