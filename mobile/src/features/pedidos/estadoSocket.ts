/**
 * Estación Burger — Estado en vivo del pedido (RF-43)
 *
 * Cliente STOMP sobre WebSocket contra el backend Spring:
 *   - Handshake:  /ws
 *   - Topic:      /topic/pedido/{codigo}
 *   - El token JWT viaja en el header Authorization del CONNECT
 *     (el backend valida que el pedido sea del usuario).
 *
 * Devuelve una función para cancelar la suscripción al desmontar.
 */

import { Client, IMessage } from '@stomp/stompjs';
import { API_BASE_URL } from '../../config/environment';
import type { PedidoCliente } from './types';

/** Deriva la URL del WebSocket desde la URL base de la API. */
export function toWebSocketUrl(baseUrl: string): string {
  const ws = baseUrl.replace(/^http/, 'ws');
  const sinApi = ws.replace(/\/api\/v1\/?$/, '');
  return `${sinApi}/ws`;
}

/**
 * Se suscribe al estado en vivo de un pedido. Llama a `onEstado` con
 * cada actualización recibida. Devuelve el `unsubscribe` (cierra el
 * cliente STOMP).
 */
export function suscribirEstadoPedido(
  codigo: string,
  accessToken: string,
  onEstado: (pedido: PedidoCliente) => void
): () => void {
  const client = new Client({
    brokerURL: toWebSocketUrl(API_BASE_URL),
    connectHeaders: { Authorization: `Bearer ${accessToken}` },
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  });

  client.onConnect = () => {
    client.subscribe(`/topic/pedido/${codigo}`, (message: IMessage) => {
      try {
        onEstado(JSON.parse(message.body) as PedidoCliente);
      } catch {
        // Ignorar mensajes malformados
      }
    });
  };

  client.activate();

  return () => {
    void client.deactivate();
  };
}
