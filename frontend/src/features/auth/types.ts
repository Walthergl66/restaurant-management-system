export interface UsuarioInfo {
  id: number; username: string; nombre: string; activo: boolean; rol: string; permisos: string[]
}
export interface AuthResponse {
  accessToken: string; refreshToken: string; expiresInSeconds: number; tokenType: string; usuario: UsuarioInfo
}
export type UserInfo = UsuarioInfo
