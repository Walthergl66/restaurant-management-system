/**
 * Estación Burger — Error de la API
 *
 * El backend responde errores con ProblemDetail (RFC 7807):
 * {
 *   "type": "urn:problem:restaurante:<tipo>",
 *   "title": "Bad Request",
 *   "detail": "Mensaje legible",
 *   "path": "/api/v1/...",
 *   "fieldErrors": { "campo": "mensaje" }   // solo en validación 400
 * }
 */

/** Mapa campo -> mensaje de validación (GlobalExceptionHandler del backend). */
export type FieldErrors = Record<string, string>;

export class ApiError extends Error {
  /** Código HTTP (0 si no hubo respuesta: sin conexión). */
  readonly status: number;
  /** URN del problema, p.ej. "urn:problem:restaurante:validacion". */
  readonly type: string;
  /** Errores de validación por campo (solo 400 de validación). */
  readonly fieldErrors: FieldErrors;

  constructor(options: {
    status: number;
    message: string;
    type?: string;
    fieldErrors?: FieldErrors;
  }) {
    super(options.message);
    this.name = 'ApiError';
    this.status = options.status;
    this.type = options.type ?? 'urn:problem:restaurante:desconocido';
    this.fieldErrors = options.fieldErrors ?? {};
  }
}

interface ProblemDetailBody {
  type?: string;
  title?: string;
  detail?: string;
  fieldErrors?: FieldErrors;
}

/**
 * Convierte el cuerpo ProblemDetail de una respuesta no-OK en ApiError.
 * `detail` es el mensaje legible; `title` es el fallback del estándar.
 */
export function problemDetailToApiError(
  status: number,
  body: unknown
): ApiError {
  const b = (body ?? {}) as ProblemDetailBody;
  const message = b.detail || b.title || `Error HTTP ${status}`;
  return new ApiError({
    status,
    type: b.type,
    message,
    fieldErrors: b.fieldErrors ?? {},
  });
}

/** Mensaje del primer error de campo, útil para formularios. */
export function firstFieldErrorMessage(
  fieldErrors: FieldErrors
): string | null {
  const entries = Object.entries(fieldErrors);
  return entries.length > 0 ? entries[0][1] : null;
}
