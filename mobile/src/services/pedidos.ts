/**
 * Estación Burger — Servicio de Pedidos (Cliente)
 */

import api from './api';
import {
  DireccionCliente,
  PedidoCliente,
  PedidoClienteLinea,
} from '../types';

export interface CreatePedidoRequest {
  lineas: PedidoClienteLinea[];
  metodoPago: string;
  metodoEntrega: string;
  direccion?: DireccionCliente;
  observaciones?: string;
  idempotencyKey: string;
}

export const pedidosService = {
  async createPedido(
    data: CreatePedidoRequest
  ): Promise<PedidoCliente> {
    return api.post('/clientes/pedidos', data);
  },

  async confirmarPedido(
    codigo: string,
    idempotencyKey: string
  ): Promise<PedidoCliente> {
    return api.post(
      `/clientes/pedidos/${codigo}/confirmar`,
      { idempotencyKey }
    );
  },

  async getPedido(codigo: string): Promise<PedidoCliente> {
    return api.get(`/clientes/pedidos/${codigo}`);
  },

  async getHistorial(page: number = 0, size: number = 10) {
    return api.get(`/clientes/historial?page=${page}&size=${size}`);
  },

  async addDireccion(direccion: DireccionCliente): Promise<DireccionCliente> {
    return api.post('/clientes/direcciones', direccion);
  },
};
