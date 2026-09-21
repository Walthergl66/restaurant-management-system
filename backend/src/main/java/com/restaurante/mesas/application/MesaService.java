package com.restaurante.mesas.application;

import com.restaurante.mesas.Mesas;
import com.restaurante.mesas.MesaResumen;
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
import java.util.Optional;

/**
 * Administración de mesas y sus estados. También implementa {@link Mesas},
 * la API pública que usa el flujo de pedidos para ocupar y liberar mesas.
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

    @Override
    @Transactional(readOnly = true)
    public Optional<MesaResumen> mesa(Long mesaId) {
        return mesaRepository.findById(mesaId)
                .map(m -> new MesaResumen(m.getId(), m.getNumero(), m.getEstado(), m.isActivo()));
    }

    @Override
    public void ocuparMesa(Long mesaId) {
        Mesa mesa = cargar(mesaId);
        // Idempotente: las adiciones abren un segundo pedido en una mesa que ya
        // está ocupada por la cuenta abierta (RF-17 a RF-19).
        if (mesa.getEstado() != EstadoMesa.OCUPADA) {
            mesa.ocupar();
        }
    }

    @Override
    public void liberarMesa(Long mesaId) {
        cargar(mesaId).liberar();
    }

    private Mesa cargar(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mesa no encontrada"));
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