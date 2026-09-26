/**
 * Estación Burger — Servicio de Pedidos (Cliente)
 * Compatible con POST /api/v1/clientes/pedidos
 */

import api from './api';
import {
  CreatePedidoRequest,
  PedidoCliente,
  DireccionCliente,
} from '../types';

export const pedidosService = {
  /**
   * Crea un pedido en BORRADOR (idempotente)
   * El backend espera: { codigo, metodoPago, metodoEntrega, direccionId, idempotencyKey, items: [{ productoId, cantidad, extraIds }] }
   */
  async createPedido(data: CreatePedidoRequest): Promise<PedidoCliente> {
    return api.post('/clientes/pedidos', data);
  },

  /**
   * Confirma un pedido (BORRADOR -> CONFIRMADO)
   * Requiere la MISMA idempotenciaKey usada en createPedido
   */
  async confirmarPedido(
    codigo: string,
    idempotencyKey: string
  ): Promise<PedidoCliente> {
    return api.post(`/clientes/pedidos/${codigo}/confirmar`, {
      idempotencyKey,
    });
  },

  /**
   * Obtiene el estado de un pedido propio
   */
  async getPedido(codigo: string): Promise<PedidoCliente> {
    return api.get(`/clientes/pedidos/${codigo}`);
  },

  /**
   * Historial paginado del cliente
   */
  async getHistorial(page: number = 0, size: number = 10) {
    return api.get(`/clientes/historial?page=${page}&size=${size}`);
  },

  /**
   * Agrega una nueva dirección para domicilio
   */
  async addDireccion(direccion: DireccionCliente): Promise<DireccionCliente> {
    return api.post('/clientes/direcciones', direccion);
  },
};
