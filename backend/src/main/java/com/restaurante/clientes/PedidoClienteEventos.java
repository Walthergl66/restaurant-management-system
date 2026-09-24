package com.restaurante.clientes;

/**
 * Constantes del canal en vivo del pedido del cliente (RF-43). Compartidas
 * entre el controller, el interceptor STOMP, el difusor y el servicio para
 * que el tipo del outbox y el destino del tópico estén en un solo lugar.
 */
public final class PedidoClienteEventos {

    /** Tipo en la tabla outbox (V4) para los avisos de cambio de estado. */
    public static final String TIPO_ESTADO = "pedido-cliente-estado";

    /** Evento guardado en el outbox (único por ahora). */
    public static final String EVENTO_ESTADO = "pedido-cliente-estado";

    /** Prefijo del tópico STOMP: /topic/pedido/{codigo}. */
    public static final String TOPIC_ESTADO = "/topic/pedido/";

    private PedidoClienteEventos() {
    }
}