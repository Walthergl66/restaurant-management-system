package com.restaurante.mesas.application;

import com.restaurante.mesas.domain.EstadoMesa;
import com.restaurante.mesas.domain.Mesa;
import com.restaurante.mesas.infrastructure.MesaRepository;
import com.restaurante.mesas.web.dto.CambiarEstadoMesaRequest;
import com.restaurante.mesas.web.dto.MesaRequest;
import com.restaurante.mesas.web.dto.MesaResponse;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Administración de mesas y sus estados. El flujo de pedidos (Fase 3)
 * ocupará y liberará mesas a través de {@link Mesa#ocupar()} y
 * {@link Mesa#liberar()}.
 */
@Service
@Transactional
public class MesaService {

    private final MesaRepository mesaRepository;

    public MesaService(MesaRepository mesaRepository) {
        this.mesaRepository = mesaRepository;
    }

    public MesaResponse crear(MesaRequest request) {
        if (mesaRepository.existsByNumero(request.numero())) {
            throw new ConflictException("Ya existe una mesa con el número " + request.numero());
        }
        Mesa mesa = new Mesa(request.numero(), request.capacidad());
        mesa.cambiarUbicacion(request.ubicacion());
        return MesaResponse.from(mesaRepository.save(mesa));
    }

    @Transactional(readOnly = true)
    public List<MesaResponse> listar(boolean soloActivas) {
        List<Mesa> mesas = soloActivas
                ? mesaRepository.findByActivoTrueOrderByNumeroAsc()
                : mesaRepository.findAll().stream().sorted((a, b) -> Integer.compare(a.getNumero(), b.getNumero())).toList();
        return mesas.stream().map(MesaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MesaResponse obtener(Long id) {
        return MesaResponse.from(mesaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mesa no encontrada")));
    }

    public MesaResponse actualizar(Long id, MesaRequest request) {
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mesa no encontrada"));
        mesa.cambiarNumero(request.numero());
        mesa.cambiarCapacidad(request.capacidad());
        mesa.cambiarUbicacion(request.ubicacion());
        return MesaResponse.from(mesa);
    }

    public MesaResponse cambiarEstado(Long id, CambiarEstadoMesaRequest request) {
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mesa no encontrada"));
        aplicarEstado(mesa, request.estado());
        return MesaResponse.from(mesa);
    }

    public void eliminar(Long id) {
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mesa no encontrada"));
        mesa.desactivar();
    }

    private void aplicarEstado(Mesa mesa, EstadoMesa estado) {
        switch (estado) {
            case OCUPADA -> mesa.ocupar();
            case LIBRE -> mesa.liberar();
            case RESERVADA -> mesa.reservar();
            case INACTIVA -> mesa.desactivar();
            default -> throw new BusinessRuleException("Estado no soportado: " + estado);
        }
    }
}