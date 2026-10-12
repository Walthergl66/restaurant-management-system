export interface EventoAuditoria {
  id: number; usuario: string; tipo: string; entidad: string
  entidadId?: string | null; detalle?: string | null; fecha: string
}
