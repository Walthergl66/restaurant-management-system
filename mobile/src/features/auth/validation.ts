/**
 * Estación Burger — Validaciones del registro de cliente
 *
 * Espejo en cliente de las reglas del backend (shared.validation): cédula
 * ecuatoriana (módulo 10), celular (09 + 8 dígitos) y contraseña segura. La
 * validación de aquí es sólo para dar feedback inmediato; el backend revalida.
 */

/** Criterios de una contraseña segura (uno por tipo de carácter + longitud). */
export interface CriteriosPassword {
  longitud: boolean;
  minuscula: boolean;
  mayuscula: boolean;
  numero: boolean;
  simbolo: boolean;
}

export type NivelPassword = 'debil' | 'media' | 'fuerte';

export interface EvaluacionPassword {
  criterios: CriteriosPassword;
  /** true sólo si cumple TODOS los criterios. */
  cumple: boolean;
  nivel: NivelPassword;
  /** Progreso 0..1 sobre los criterios cumplidos. */
  porcentaje: number;
}

const LONGITUD_MINIMA = 8;
const LONGITUD_MAXIMA = 72;
const COEFICIENTES_CEDULA = [2, 1, 2, 1, 2, 1, 2, 1, 2];

/** Cédula ecuatoriana de 10 dígitos con dígito verificador (módulo 10). */
export function esCedulaValida(cedula: string): boolean {
  const c = (cedula ?? '').trim();
  if (!/^\d{10}$/.test(c)) return false;

  const provincia = Number(c.slice(0, 2));
  if (provincia < 1 || (provincia > 24 && provincia !== 30)) return false;
  if (Number(c[2]) > 5) return false;

  let suma = 0;
  for (let i = 0; i < 9; i++) {
    let valor = Number(c[i]) * COEFICIENTES_CEDULA[i];
    if (valor > 9) valor -= 9;
    suma += valor;
  }
  const verificador = (10 - (suma % 10)) % 10;
  return verificador === Number(c[9]);
}

/** Celular de Ecuador: 10 dígitos que empiezan con 09. */
export function esCelularValido(celular: string): boolean {
  return /^09\d{8}$/.test((celular ?? '').trim());
}

/** Correo con formato válido y a lo sumo 50 caracteres (límite del username). */
export function esCorreoValido(correo: string): boolean {
  const c = (correo ?? '').trim();
  return c.length <= 50 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(c);
}

/** Evalúa los criterios de la contraseña y calcula su rango de seguridad. */
export function evaluarPassword(password: string): EvaluacionPassword {
  const criterios: CriteriosPassword = {
    longitud: password.length >= LONGITUD_MINIMA && password.length <= LONGITUD_MAXIMA,
    minuscula: /[a-z]/.test(password),
    mayuscula: /[A-Z]/.test(password),
    numero: /\d/.test(password),
    simbolo: /[^\sa-zA-Z0-9]/.test(password),
  };

  const cumplidos = Object.values(criterios).filter(Boolean).length;
  const cumple = cumplidos === 5;
  const nivel: NivelPassword = cumple ? 'fuerte' : cumplidos >= 3 ? 'media' : 'debil';

  return { criterios, cumple, nivel, porcentaje: cumplidos / 5 };
}

/** Etiqueta legible del rango de seguridad. */
export function etiquetaNivel(nivel: NivelPassword): string {
  switch (nivel) {
    case 'fuerte':
      return 'Fuerte';
    case 'media':
      return 'Media';
    default:
      return 'Débil';
  }
}
