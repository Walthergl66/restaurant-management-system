/**
 * Estación Burger — Tipos del módulo delivery (repartidor)
 *
 * Espejo EXACTO de los records del backend
 * (com.restaurante.delivery.application + DeliveryRepartidorController).
 * El móvil habla solo con nuestro backend; nunca con ORS.
 */

/** EntregaResumen — una asignación activa del repartidor con datos de entrega (PII). */
export interface EntregaResumen {
  asignacionId: number;
  pedidoClienteId: number;
  recorridoId: number | null;
  estadoAsignacion: string;
  codigo: string;
  clienteNombre: string;
  direccion: string;
  telefono: string | null;
}

/** ParadaResumen — una parada dentro de un recorrido (orden 0-based). */
export interface ParadaResumen {
  paradaId: number;
  asignacionId: number;
  pedidoClienteId: number | null;
  orden: number;
  estado: string;
  codigo: string | null;
  direccion: string | null;
}

/** RecorridoResumen — un recorrido con su estado logístico y paradas en orden. */
export interface RecorridoResumen {
  recorridoId: number;
  repartidorId: number;
  tipo: string;
  estado: string;
  criterio: string;
  distanciaTotalM: number | null;
  duracionTotalS: number | null;
  iniciadoAt: string | null;
  finalizadoAt: string | null;
  paradas: ParadaResumen[];
}

/** MisEntregas — asignaciones activas + recorridos abiertos del repartidor. */
export interface MisEntregas {
  repartidorId: number;
  asignaciones: EntregaResumen[];
  recorridos: RecorridoResumen[];
}

/** RutaCalculada — geometría codificada + totales (ORS Directions). */
export interface RutaCalculada {
  geometria: string;
  distanciaM: number;
  duracionS: number;
}

/** ParadaPropuesta — una parada del orden propuesto por ORS Optimization. */
export interface ParadaPropuesta {
  orden: number;
  asignacionId: number;
  pedidoClienteId: number | null;
  codigo: string | null;
  direccion: string | null;
}

/** PropuestaMultientrega — orden eficiente + geometría + totales (no persiste). */
export interface PropuestaMultientrega {
  paradas: ParadaPropuesta[];
  criterio: string;
  geometria: string;
  distanciaTotalM: number;
  duracionTotalS: number;
}

/** ParadaResponse — estado de una parada tras operarla (entregada/incidencia). */
export interface ParadaResponse {
  paradaId: number;
  asignacionId: number;
  pedidoClienteId: number | null;
  orden: number;
  estado: string;
  codigo: string | null;
  direccion: string | null;
}

/** Criterio de optimización del orden de paradas. */
export type CriterioOptimizacion = 'DURACION' | 'DISTANCIA';

/** Tipo de recorrido. */
export type TipoRecorrido = 'INDIVIDUAL' | 'MULTIPLE';
