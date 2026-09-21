package com.restaurante.configuracion.application;

import com.restaurante.configuracion.domain.Impresora;
import com.restaurante.configuracion.infrastructure.ImpresoraRepository;
import com.restaurante.configuracion.web.dto.ImpresoraRequest;
import com.restaurante.configuracion.web.dto.ImpresoraResponse;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administración de impresoras térmicas del local.
 */
@Service
@Transactional
public class ImpresoraService {

    private final ImpresoraRepository impresoraRepository;

    public ImpresoraService(ImpresoraRepository impresoraRepository) {
        this.impresoraRepository = impresoraRepository;
    }

    public ImpresoraResponse crear(ImpresoraRequest request) {
        if (impresoraRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ConflictException("Ya existe una impresora llamada '" + request.nombre() + "'");
        }
        Impresora impresora = new Impresora(request.nombre(), request.tipo());
        aplicarCampos(impresora, request);
        return ImpresoraResponse.from(impresoraRepository.save(impresora));
    }

    @Transactional(readOnly = true)
    public Page<ImpresoraResponse> listar(Pageable pageable) {
        return impresoraRepository.findAll(pageable).map(ImpresoraResponse::from);
    }

    @Transactional(readOnly = true)
    public ImpresoraResponse obtener(Long id) {
        return ImpresoraResponse.from(impresoraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Impresora no encontrada")));
    }

    public ImpresoraResponse actualizar(Long id, ImpresoraRequest request) {
        Impresora impresora = impresoraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Impresora no encontrada"));
        impresora.cambiarNombre(request.nombre());
        impresora.cambiarTipo(request.tipo());
        aplicarCampos(impresora, request);
        return ImpresoraResponse.from(impresora);
    }

    public void eliminar(Long id) {
        Impresora impresora = impresoraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Impresora no encontrada"));
        // Desactivación lógica: se conserva el historial de impresión futura.
        impresora.desactivar();
    }

    private void aplicarCampos(Impresora impresora, ImpresoraRequest request) {
        impresora.cambiarIp(request.ip());
        impresora.cambiarArea(request.area());
        if (request.puerto() != null) {
            impresora.cambiarPuerto(request.puerto());
        }
    }
}