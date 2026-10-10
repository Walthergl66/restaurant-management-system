/**
 * Estación Burger — Tipos de autenticación
 * Contrato: AuthController + AuthResponse + UsuarioInfo del backend.
 */

/** UsuarioInfo — vista pública del usuario (nunca expone el hash). */
export interface UsuarioInfo {
  id: number;
  username: string;
  nombre: string;
  activo: boolean;
  rol: string | null;
  permisos: string[];
}

/** AuthResponse — resultado de login/refresh. */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  tokenType: string;
  usuario: UsuarioInfo;
}

/** LoginRequest — POST /api/v1/auth/login. */
export interface LoginRequest {
  username: string;
  password: string;
}

/** RegistroRequest — POST /api/v1/auth/registro (RF-45). El correo es el username. */
export interface RegistroRequest {
  username: string;
  nombre: string;
  cedula: string;
  celular: string;
  password: string;
}

/** RegistroPendienteResponse — alta creada, pendiente de verificar el correo. */
export interface RegistroPendienteResponse {
  username: string;
  emailVerificado: boolean;
  mensaje: string;
}

/** VerificarEmailRequest — POST /api/v1/auth/verificar-email (código de 6 dígitos). */
export interface VerificarEmailRequest {
  username: string;
  codigo: string;
}

/** ReenviarVerificacionRequest — POST /api/v1/auth/reenviar-verificacion. */
export interface ReenviarVerificacionRequest {
  username: string;
}

/** SolicitarRecuperacionRequest — POST /api/v1/auth/solicitar-recuperacion. */
export interface SolicitarRecuperacionRequest {
  username: string;
}

/** RestablecerPasswordRequest — POST /api/v1/auth/restablecer-password. */
export interface RestablecerPasswordRequest {
  token: string;
  nuevaPassword: string;
}

/** RefreshTokenRequest — POST /api/v1/auth/refresh y /auth/logout. */
export interface RefreshTokenRequest {
  refreshToken: string;
}

/** Sesión persistida localmente. */
export interface StoredAuth {
  accessToken: string;
  refreshToken: string;
  usuario: UsuarioInfo;
}
