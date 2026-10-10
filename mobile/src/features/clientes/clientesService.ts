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

const ENDPOINTS = {
  perfil: '/clientes/perfil',
} as const;

export const clientesService = {
  /** Perfil del cliente autenticado (cédula, celular, nombre). */
  async perfil(): Promise<ClientePerfil> {
    return apiClient.get<ClientePerfil>(ENDPOINTS.perfil);
  },
};

