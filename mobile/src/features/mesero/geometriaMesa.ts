/**
 * Geometría para dibujar una mesa con sus sillas (plano del salón). Lógica pura, sin React Native:
 * recibe la capacidad y devuelve el cuerpo de la mesa y la posición de cada silla dentro de un recuadro.
 */

export type FormaMesa = 'cuadrada' | 'redonda' | 'alargada';

export interface Rect {
  x: number;
  y: number;
  w: number;
  h: number;
}

export interface DisenoMesa {
  forma: FormaMesa;
  cuerpo: Rect;
  sillas: Rect[];
  /** Tamaño total del recuadro (cuerpo + sillas). */
  ancho: number;
  alto: number;
}

/** Máximo de sillas que se dibujan (las mesas más grandes se representan igual). */
export const MAX_SILLAS = 12;

const SILLA_LARGO = 18;
const SILLA_GROSOR = 8;
const SEPARACION = 3;
const MARGEN = SILLA_GROSOR + SEPARACION;

/** Reparte `cantidad` sillas a lo largo de un lado de longitud `largo`, sin superponerlas. */
function repartir(cantidad: number, inicio: number, largo: number): number[] {
  const hueco = largo / Math.max(cantidad, 1);
  return Array.from({ length: cantidad }, (_, i) => inicio + hueco * (i + 0.5) - SILLA_LARGO / 2);
}

export function disenarMesa(capacidad: number): DisenoMesa {
  const n = Number.isFinite(capacidad) ? Math.min(Math.max(Math.floor(capacidad), 1), MAX_SILLAS) : 1;

  if (n >= 5) {
    const arriba = Math.ceil(n / 2);
    const abajo = Math.floor(n / 2);
    const ancho = Math.max(120, arriba * (SILLA_LARGO + SEPARACION) + 12);
    const cuerpo: Rect = { x: MARGEN, y: MARGEN, w: ancho, h: 52 };
    const sillas: Rect[] = [
      ...repartir(arriba, cuerpo.x, cuerpo.w).map(x => ({ x, y: 0, w: SILLA_LARGO, h: SILLA_GROSOR })),
      ...repartir(abajo, cuerpo.x, cuerpo.w).map(x => ({ x, y: cuerpo.y + cuerpo.h + SEPARACION, w: SILLA_LARGO, h: SILLA_GROSOR })),
    ];
    return { forma: 'alargada', cuerpo, sillas, ancho: ancho + 2 * MARGEN, alto: cuerpo.h + 2 * MARGEN };
  }

  const lado = n <= 2 ? 52 : 64;
  const cuerpo: Rect = { x: MARGEN, y: MARGEN, w: lado, h: lado };
  const centroX = cuerpo.x + lado / 2 - SILLA_LARGO / 2;
  const centroY = cuerpo.y + lado / 2 - SILLA_LARGO / 2;
  const arriba: Rect = { x: centroX, y: 0, w: SILLA_LARGO, h: SILLA_GROSOR };
  const abajo: Rect = { x: centroX, y: cuerpo.y + lado + SEPARACION, w: SILLA_LARGO, h: SILLA_GROSOR };
  const izquierda: Rect = { x: 0, y: centroY, w: SILLA_GROSOR, h: SILLA_LARGO };
  const derecha: Rect = { x: cuerpo.x + lado + SEPARACION, y: centroY, w: SILLA_GROSOR, h: SILLA_LARGO };
  const sillas = [arriba, abajo, izquierda, derecha].slice(0, n);
  const total = lado + 2 * MARGEN;
  return { forma: n <= 2 ? 'cuadrada' : 'redonda', cuerpo, sillas, ancho: total, alto: total };
}
