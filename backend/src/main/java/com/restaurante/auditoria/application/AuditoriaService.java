package com.restaurante.auditoria.application;

import com.restaurante.auditoria.AuditoriaResumen;
import com.restaurante.auditoria.domain.EventoAuditoria;
import com.restaurante.auditoria.infrastructure.AuditoriaRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Historial de operaciones (RF-52). El registro se hace SIEMPRE en una
 * transacción propia ({@code REQUIRES_NEW}) para que la auditoría quede
 * aunque la transacción del negocio se revierta (RNF-10/RNF-11).
 */
@Service
@Transactional
public class AuditoriaService {

    private final AuditoriaRepository repository;

    public AuditoriaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String tipo, String entidad, String entidadId, String detalle) {
        repository.save(new EventoAuditoria(usuarioActual(), tipo, entidad, entidadId, detalle));
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResumen> listar(Instant desde, Instant hasta, String entidad) {
        return repository.findByFechaBetweenOrderByFechaDesc(
                        desde, hasta == null ? Instant.now().plusSeconds(60) : hasta)
                .stream()
                .filter(e -> entidad == null || entidad.equals(e.getEntidad()))
                .map(e -> new AuditoriaResumen(
                        e.getId(), e.getUsuario(), e.getTipo(), e.getEntidad(),
                        e.getEntidadId(), e.getDetalle(), e.getFecha()))
                .toList();
    }

    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null
                || "anonymousUser".equals(auth.getPrincipal()) || "sistema".equals(auth.getPrincipal())) {
            return "sistema";
        }
        return auth.getName();
    }
}