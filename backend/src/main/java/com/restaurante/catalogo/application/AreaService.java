package com.restaurante.catalogo.application;

import com.restaurante.catalogo.domain.Area;
import com.restaurante.catalogo.infrastructure.AreaRepository;
import com.restaurante.catalogo.web.dto.AreaRequest;
import com.restaurante.catalogo.web.dto.AreaResponse;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Administración de áreas de preparación.
 */
@Service
@Transactional
public class AreaService {

    private final AreaRepository areaRepository;

    public AreaService(AreaRepository areaRepository) {
        this.areaRepository = areaRepository;
    }

    public AreaResponse crear(AreaRequest request) {
        if (areaRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ConflictException("Ya existe un área llamada '" + request.nombre() + "'");
        }
        Area area = new Area(request.nombre());
        area.cambiarDescripcion(request.descripcion());
        return AreaResponse.from(areaRepository.save(area));
    }

    @Transactional(readOnly = true)
    public List<AreaResponse> listar(boolean soloActivas) {
        return areaRepository.findAll().stream()
                .filter(a -> !soloActivas || a.isActivo())
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .map(AreaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AreaResponse obtener(Long id) {
        return AreaResponse.from(areaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Área no encontrada")));
    }

    public AreaResponse actualizar(Long id, AreaRequest request) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Área no encontrada"));
        area.cambiarNombre(request.nombre());
        area.cambiarDescripcion(request.descripcion());
        return AreaResponse.from(area);
    }

    public void eliminar(Long id) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Área no encontrada"));
        area.desactivar();
    }
}