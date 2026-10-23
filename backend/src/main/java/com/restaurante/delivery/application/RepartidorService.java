package com.restaurante.delivery.application;

import com.restaurante.auditoria.Auditoria;
import com.restaurante.delivery.domain.EstadoRepartidor;
import com.restaurante.delivery.domain.Repartidor;
import com.restaurante.delivery.infrastructure.RepartidorRepository;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.usuarios.Usuarios;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * Gestión del estado operativo de los repartidores, SEPARADO del rol DELIVERY
 * (que vive en seguridad). El rol dice quién PUEDE ser repartidor; esta capa
 * decide si HOY opera (PENDIENTE/HABILITADO/SUSPENDIDO).
 *
 * <p>El administrador habilita o suspende repartidores; el repartidor solo
 * opera cuando está HABILITADO. El método {@link #repartidorHabilitadoDelUsuarioActual()}
 * es la base del guard operativo que las fases siguientes (5/9) usarán para
 * asegurar que (a) el usuario es un repartidor habilitado y (b) opera sobre lo
 * suyo.
 */
@Service
@Transactional
public class RepartidorService {

    private static final String ROL_DELIVERY = "DELIVERY";
    private static final String CATEGORIA_AUDITORIA = "DELIVERY";

    private final RepartidorRepository repartidores;
    private final Usuarios usuarios;
    private final Auditoria auditoria;
    private final ObjectMapper objectMapper;

    public RepartidorService(RepartidorRepository repartidores, Usuarios usuarios,
                             Auditoria auditoria, ObjectMapper objectMapper) {
        this.repartidores = repartidores;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.objectMapper = objectMapper;
    }

    /**
     * Habilita a un repartidor. Si no existía el registro operativo, lo crea
     * (nace PENDIENTE y pasa a HABILITADO). Solo sobre usuarios con rol
     * DELIVERY: si el usuario no es DELIVERY -> 422.
     */
    public RepartidorResumen habilitar(Long usuarioId, String actorAdmin) {
        validarEsDelivery(usuarioId);
        Repartidor repartidor = repartidores.findByUsuarioId(usuarioId)
                .orElseGet(() -> repartidores.save(new Repartidor(usuarioId)));
        repartidor.habilitar(actorAdmin);
        repartidores.save(repartidor);
        auditoria.registrar("REPARTIDOR_HABILITADO", "REPARTIDOR",
                String.valueOf(repartidor.getId()),
                json(Map.of("usuarioId", String.valueOf(usuarioId))), CATEGORIA_AUDITORIA);
        return RepartidorResumen.de(repartidor);
    }

    /**
     * Suspende a un repartidor HABILITADO, dejándolo fuera de ruta sin
     * borrarlo.
     */
    public RepartidorResumen suspender(Long usuarioId, String actorAdmin) {
        Repartidor repartidor = repartidores.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new BusinessRuleException(
                        "El usuario no es un repartidor registrado"));
        repartidor.suspender();
        repartidores.save(repartidor);
        auditoria.registrar("REPARTIDOR_SUSPENDIDO", "REPARTIDOR",
                String.valueOf(repartidor.getId()),
                json(Map.of("usuarioId", String.valueOf(usuarioId))), CATEGORIA_AUDITORIA);
        return RepartidorResumen.de(repartidor);
    }

    @Transactional(readOnly = true)
    public List<RepartidorResumen> listarRepartidores() {
        return repartidores.findAllByOrderByIdAsc().stream()
                .map(RepartidorResumen::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RepartidorResumen> listarHabilitados() {
        return repartidores.findByEstadoOrderByIdAsc(EstadoRepartidor.HABILITADO).stream()
                .map(RepartidorResumen::de)
                .toList();
    }

    /**
     * Guard operativo reutilizable por las fases siguientes: devuelve el
     * repartidor del usuario autenticado y falla si no existe (403) o no está
     * HABILITADO (422). Un repartidor PENDIENTE o SUSPENDIDO no opera aunque
     * tenga el permiso: el estado operativo es independiente del permiso.
     */
    @Transactional(readOnly = true)
    public Repartidor repartidorHabilitadoDelUsuarioActual() {
        Long usuarioId = usuarioActualId();
        Repartidor repartidor = repartidores.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new AccessDeniedException(
                        "El usuario actual no es un repartidor"));
        if (!repartidor.estaHabilitado()) {
            throw new BusinessRuleException(
                    "El repartidor no está habilitado para operar");
        }
        return repartidor;
    }

    private void validarEsDelivery(Long usuarioId) {
        Usuarios.UsuarioResumen usuario = usuarios.porId(usuarioId)
                .orElseThrow(() -> new BusinessRuleException(
                        "El usuario no existe"));
        if (!ROL_DELIVERY.equals(usuario.rolCodigo())) {
            throw new BusinessRuleException(
                    "Solo un usuario con rol DELIVERY puede habilitarse como repartidor");
        }
    }

    private String json(Map<String, String> detalle) {
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (Exception e) {
            return "{\"detalle\":\"no serializable\"}";
        }
    }

    private Long usuarioActualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("No autenticado");
        }
        return usuarios.porUsername(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado desconocido"))
                .id();
    }
}
