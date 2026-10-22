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
 * Historial de operaciones (RF-52). El registro se hace en una transacción
 * propia ({@code REQUIRES_NEW}) porque el listener que lo dispara tras
 * el commit del negocio ({@code AFTER_COMMIT}): así la escritura no depende de
 * la transacción original y un fallo al auditar no invalida el commit (RNF-10/RNF-11).
 */
@Service
@Transactional
public class AuditoriaService implements com.restaurante.auditoria.Auditoria {

    private final AuditoriaRepository repository;
    private final org.springframework.beans.factory.ObjectProvider<AuditoriaHttpContext> auditoriaHttpContextProvider;
    private AuditoriaHttpContext cached;

    public AuditoriaService(AuditoriaRepository repository,
                            org.springframework.beans.factory.ObjectProvider<AuditoriaHttpContext> auditoriaHttpContextProvider) {
        this.repository = repository;
        this.auditoriaHttpContextProvider = auditoriaHttpContextProvider;
    }

    private AuditoriaHttpContext contexto() {
        if (cached != null) {
            return cached;
        }
        try {
            cached = auditoriaHttpContextProvider.getIfAvailable();
        } catch (Exception ignored) {
            cached = null;
        }
        if (cached == null) {
            cached = new AuditoriaHttpContext();
        }
        return cached;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String tipo, String entidad, String entidadId, String detalle) {
        registrar(tipo, entidad, entidadId, detalle, "NEGOCIO");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String tipo, String entidad, String entidadId, String detalle, String categoria) {
        AuditoriaHttpContext ctx = contexto();
        repository.save(new EventoAuditoria(
                usuarioActual(),
                tipo,
                entidad,
                entidadId,
                detalle,
                categoria,
                ctx.getIp(),
                ctx.getUserAgent(),
                ctx.getRequestId()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String usuario, String tipo, String entidad, String entidadId, String detalle, String categoria) {
        AuditoriaHttpContext ctx = contexto();
        repository.save(new EventoAuditoria(
                usuario,
                tipo,
                entidad,
                entidadId,
                detalle,
                categoria,
                ctx.getIp(),
                ctx.getUserAgent(),
                ctx.getRequestId()));
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResumen> listar(Instant desde, Instant hasta, String entidad, String categoria) {
        Instant fin = hasta == null ? Instant.now() : hasta;
        List<EventoAuditoria> eventos;
        if (entidad != null && categoria != null) {
            eventos = repository.findByFechaBetweenAndEntidadAndCategoriaOrderByFechaDesc(desde, fin, entidad, categoria);
        } else if (entidad != null) {
            eventos = repository.findByFechaBetweenAndEntidadOrderByFechaDesc(desde, fin, entidad);
        } else if (categoria != null) {
            eventos = repository.findByFechaBetweenAndCategoriaOrderByFechaDesc(desde, fin, categoria);
        } else {
            eventos = repository.findByFechaBetweenOrderByFechaDesc(desde, fin);
        }
        return eventos.stream()
                .map(e -> new AuditoriaResumen(
                        e.getId(), e.getUsuario(), e.getTipo(), e.getEntidad(),
                        e.getEntidadId(), e.getDetalle(), e.getFecha(),
                        e.getCategoria(), e.getIp(), e.getUserAgent(), e.getRequestId()))
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