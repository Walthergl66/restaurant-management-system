package com.restaurante.mesas.application;

import com.restaurante.mesas.Mesas;
import com.restaurante.mesas.MesaResumen;
import com.restaurante.mesas.domain.Mesa;
import com.restaurante.mesas.infrastructure.MesaRepository;
import com.restaurante.mesas.web.dto.HabilitacionMesaRequest;
import com.restaurante.mesas.web.dto.MesaRequest;
import com.restaurante.mesas.web.dto.MesaResponse;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Administración del catálogo de mesas. También implementa {@link Mesas},
 * la API pública que valida la habilitación al crear pedidos.
 */
@Service
@Transactional
public class MesaService implements Mesas {

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
        if (mesaRepository.existsByNumeroAndIdNot(request.numero(), id)) {
            throw new ConflictException("Ya existe una mesa con el número " + request.numero());
        }
        mesa.cambiarNumero(request.numero());
        mesa.cambiarCapacidad(request.capacidad());
        mesa.cambiarUbicacion(request.ubicacion());
        return MesaResponse.from(mesa);
    }

    public MesaResponse cambiarHabilitacion(Long id, HabilitacionMesaRequest request) {
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mesa no encontrada"));
        if (request.habilitada()) {
            mesa.habilitar();
        } else {
            mesa.deshabilitar();
        }
        return MesaResponse.from(mesa);
    }

    public void eliminar(Long id) {
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mesa no encontrada"));
        mesa.deshabilitar();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MesaResumen> mesa(Long mesaId) {
        return mesaRepository.findById(mesaId)
                .map(m -> new MesaResumen(m.getId(), m.getNumero(), m.getEstado(), m.isActivo()));
    }

    @Override
    @Transactional(readOnly = true)
    public void validarHabilitadaParaNuevoPedido(Long mesaId) {
        Mesa mesa = cargar(mesaId);
        if (!mesa.isActivo()) {
            throw new BusinessRuleException("La mesa está deshabilitada para nuevos pedidos");
        }
    }

    private Mesa cargar(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mesa no encontrada"));
    }
}
