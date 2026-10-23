/**
 * Estación Burger — Servicio de pedidos del cliente
 * Contrato: ClientesController (/api/v1/clientes, autenticado).
 */

import { apiClient } from '@/shared/api/apiClient';
import type {
  CarritoCliente,
  ConfirmarPedidoRequest,
  CrearPedidoRequest,
  DireccionCliente,
  MetodoPagoCliente,
  NuevaDireccionRequest,
  NuevoMetodoPagoRequest,
  Page,
  PedidoCliente,
  SeguimientoEntrega,
} from './types';

const ENDPOINTS = {
  carrito: '/clientes/carrito',
  pedidos: '/clientes/pedidos',
  pedido: (codigo: string) => `/clientes/pedidos/${codigo}`,
  confirmar: (codigo: string) => `/clientes/pedidos/${codigo}/confirmar`,
  seguimiento: (codigo: string) =>
    `/clientes/pedidos/${codigo}/seguimiento`,
  confirmarRecepcion: (codigo: string) =>
    `/clientes/pedidos/${codigo}/confirmar-recepcion`,
  historial: '/clientes/historial',
  direcciones: '/clientes/direcciones',
  metodosPago: '/clientes/metodos-pago',
  metodoPago: (id: number) => `/clientes/metodos-pago/${id}`,
} as const;

export const pedidosService = {
  /** Carrito del cliente: borradores (BORRADOR) con total congelado. */
  async getCarrito(): Promise<CarritoCliente> {
    return apiClient.get<CarritoCliente>(ENDPOINTS.carrito);
  },

  /**
   * Crea un pedido en BORRADOR. Idempotente: misma (codigo +
   * idempotencyKey) devuelve el mismo pedido; clave distinta → 409.
   */
  async crearPedido(pedido: CrearPedidoRequest): Promise<PedidoCliente> {
    return apiClient.post<PedidoCliente>(ENDPOINTS.pedidos, pedido);
  },

  /**
   * Confirma el pedido (BORRADOR → CONFIRMADO) congelando el total.
   * Requiere la MISMA idempotencyKey con la que se creó.
   */
  async confirmarPedido(
    codigo: string,
    idempotencyKey: string
  ): Promise<PedidoCliente> {
    const body: ConfirmarPedidoRequest = { idempotencyKey };
    return apiClient.post<PedidoCliente>(ENDPOINTS.confirmar(codigo), body);
  },

  /** Snapshot del estado de un pedido propio (el push en vivo es vía WebSocket). */
  async getPedido(codigo: string): Promise<PedidoCliente> {
    return apiClient.get<PedidoCliente>(ENDPOINTS.pedido(codigo));
  },

  /**
   * Seguimiento logístico y de doble confirmación de un pedido propio.
   * Consulta ligera de respaldo del estado en vivo del socket.
   */
  async getSeguimiento(codigo: string): Promise<SeguimientoEntrega> {
    return apiClient.get<SeguimientoEntrega>(ENDPOINTS.seguimiento(codigo));
  },

  /**
   * Confirma la recepción del pedido por parte del cliente (2.ª de las
   * dos confirmaciones). Idempotente vía header Idempotency-Key: un
   * reintento con la misma clave no duplica la confirmación.
   * 403 si el pedido es ajeno, 422 si el repartidor aún no entregó.
   */
  async confirmarRecepcion(
    codigo: string,
    idempotencyKey: string
  ): Promise<SeguimientoEntrega> {
    return apiClient.request<SeguimientoEntrega>(
      ENDPOINTS.confirmarRecepcion(codigo),
      {
        method: 'POST',
        headers: { 'Idempotency-Key': idempotencyKey },
      }
    );
  },

  /**
   * Historial paginado del cliente (últimos primeros).
   * El backend limita size a [1, 100].
   */
  async getHistorial(page = 0, size = 20): Promise<Page<PedidoCliente>> {
    return apiClient.get<Page<PedidoCliente>>(
      `${ENDPOINTS.historial}?page=${page}&size=${size}`
    );
  },

  /** Registra una nueva dirección para domicilio. */
  async agregarDireccion(
    direccion: NuevaDireccionRequest
  ): Promise<DireccionCliente> {
    return apiClient.post<DireccionCliente>(
      ENDPOINTS.direcciones,
      direccion
    );
  },

  /** Direcciones activas del cliente (RF-42). */
  async getDirecciones(): Promise<DireccionCliente[]> {
    return apiClient.get<DireccionCliente[]>(ENDPOINTS.direcciones);
  },

  /** Métodos de pago guardados del cliente (RF-45). */
  async getMetodosPago(): Promise<MetodoPagoCliente[]> {
    return apiClient.get<MetodoPagoCliente[]>(ENDPOINTS.metodosPago);
  },

  /** Guarda un método de pago (solo metadata, nunca PAN/CVV). */
  async agregarMetodoPago(
    metodo: NuevoMetodoPagoRequest
  ): Promise<MetodoPagoCliente> {
    return apiClient.post<MetodoPagoCliente>(ENDPOINTS.metodosPago, metodo);
  },

  /** Elimina (baja lógica) un método de pago guardado. */
  async eliminarMetodoPago(id: number): Promise<void> {
    await apiClient.delete(ENDPOINTS.metodoPago(id));
  },
};
