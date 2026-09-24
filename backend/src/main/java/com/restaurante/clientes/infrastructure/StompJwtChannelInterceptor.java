package com.restaurante.clientes.infrastructure;

import com.restaurante.clientes.Clientes;
import com.restaurante.clientes.PedidoClienteEventos;
import com.restaurante.usuarios.JwtValidador;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * Interceptor del canal STOMP (RF-43):
 * <ul>
 *   <li>CONNECT: valida el JWT (firma, vigencia y versión de sesión vía
 *       {@link JwtValidador}) y lo fija como usuario de la sesión.</li>
 *   <li>SUBSCRIBE: solo el propietario del pedido puede suscribirse al tópico
 *       {@code /topic/pedido/{codigo}} (404 si no es suyo, sin delatar códigos
 *       ajenos — A-01).</li>
 * </ul>
 */
@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    /** El constructor/máximo del CONNECT se guarda en los atributos de la
     *  sesión WebSocket para que los SUBSCRIBE posteriores lo recuperen. */
    static final String ATTR_PRINCIPAL = StompJwtChannelInterceptor.class.getName() + ".principal";

    private final JwtValidador jwtValidador;
    private final Clientes clientes;

    public StompJwtChannelInterceptor(JwtValidador jwtValidador, Clientes clientes) {
        this.jwtValidador = jwtValidador;
        this.clientes = clientes;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand comando = accessor.getCommand();
        if (StompCommand.CONNECT.equals(comando)) {
            Principal principal = autenticar(accessor);
            accessor.setUser(principal);
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put(ATTR_PRINCIPAL, principal);
            }
        } else if (StompCommand.SUBSCRIBE.equals(comando)) {
            verificarPropietario(accessor);
        }
        return message;
    }

    private Principal autenticar(StompHeaderAccessor accessor) {
        String auth = accessor.getFirstNativeHeader("Authorization");
        var acceso = jwtValidador.validar(auth);
        if (acceso.isEmpty()) {
            throw new MessagingException("No autorizado");
        }
        List<GrantedAuthority> autoridades = new ArrayList<>();
        acceso.get().permisos().forEach(p -> autoridades.add(new SimpleGrantedAuthority(p)));
        if (acceso.get().rol() != null && !acceso.get().rol().isBlank()) {
            autoridades.add(new SimpleGrantedAuthority("ROLE_" + acceso.get().rol()));
        }
        return new UsernamePasswordAuthenticationToken(acceso.get().username(), null, autoridades);
    }

    private void verificarPropietario(StompHeaderAccessor accessor) {
        String destino = accessor.getDestination();
        if (destino == null || !destino.startsWith(PedidoClienteEventos.TOPIC_ESTADO)) {
            return;
        }
        String codigo = destino.substring(PedidoClienteEventos.TOPIC_ESTADO.length());
        if (codigo.isBlank()) {
            return;
        }
        Principal principal = accessor.getUser();
        if (principal == null && accessor.getSessionAttributes() != null) {
            principal = (Principal) accessor.getSessionAttributes().get(ATTR_PRINCIPAL);
        }
        if (principal == null || principal.getName() == null) {
            throw new MessagingException("No autenticado");
        }
        clientes.pedidoSPIporCodigo(clientes.resolverClienteId(principal.getName()), codigo);
    }
}