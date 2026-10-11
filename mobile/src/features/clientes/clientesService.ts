/**
 * Estación Burger — Servicio del perfil del cliente
 * Contrato: GET /api/v1/clientes/perfil
 */

import { apiClient } from '../../core/api/apiClient';

export interface ClientePerfil {
  id: number;
  usuarioId: number;
  nombre: string;
  cedula: string;
  telefono: string;
}

export interface ActualizarPerfil {
  celular: string;
  correo: string;
}

const ENDPOINTS = {
  perfil: '/clientes/perfil',
} as const;

export const clientesService = {
  /** Perfil del cliente autenticado (cédula, celular, nombre). */
  async perfil(): Promise<ClientePerfil> {
    return apiClient.get<ClientePerfil>(ENDPOINTS.perfil);
  },

  /** Actualiza celular y/o correo (correo requiere re-verificación). */
  async actualizarPerfil(datos: ActualizarPerfil): Promise<ClientePerfil> {
    return apiClient.patch<ClientePerfil>(ENDPOINTS.perfil, datos);
  },
};

