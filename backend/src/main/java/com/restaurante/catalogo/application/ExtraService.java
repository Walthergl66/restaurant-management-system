package com.restaurante.catalogo.application;

import com.restaurante.catalogo.domain.Extra;
import com.restaurante.catalogo.domain.TextoCatalogo;
import com.restaurante.catalogo.infrastructure.ExtraRepository;
import com.restaurante.catalogo.web.dto.ExtraRequest;
import com.restaurante.catalogo.web.dto.HabilitacionCatalogoRequest;
import com.restaurante.catalogo.web.dto.ExtraResponse;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Administración de extras (adiciones/toppings) del catálogo.
 */
@Service
@Transactional
public class ExtraService {

    private final ExtraRepository extraRepository;

    public ExtraService(ExtraRepository extraRepository) {
        this.extraRepository = extraRepository;
    }

    public ExtraResponse crear(ExtraRequest request) {
        validarNombreDisponible(request.nombre(), null);
        Extra extra = new Extra(request.nombre(), Money.of(request.precio()));
        extra.cambiarDescripcion(request.descripcion());
        return ExtraResponse.from(extraRepository.save(extra));
    }

    @Transactional(readOnly = true)
    public List<ExtraResponse> listar(boolean soloActivos) {
        return extraRepository.findAll().stream()
                .filter(e -> !soloActivos || e.isActivo())
                .sorted((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()))
                .map(ExtraResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExtraResponse obtener(Long id) {
        return ExtraResponse.from(extraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Extra no encontrado")));
    }

    public ExtraResponse actualizar(Long id, ExtraRequest request) {
        Extra extra = extraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Extra no encontrado"));
        validarNombreDisponible(request.nombre(), id);
        extra.cambiarNombre(request.nombre());
        extra.cambiarDescripcion(request.descripcion());
        extra.cambiarPrecio(Money.of(request.precio()));
        return ExtraResponse.from(extra);
    }

    public ExtraResponse cambiarHabilitacion(Long id, HabilitacionCatalogoRequest request) {
        Extra extra = extraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Registro de catálogo no encontrado"));
        if (request.habilitada()) {
            validarNombreDisponible(extra.getNombre(), id);
            extra.activar();
        } else {
            extra.desactivar();
        }
        return ExtraResponse.from(extra);
    }

    private void validarNombreDisponible(String nombre, Long id) {
        nombre = TextoCatalogo.normalizar(nombre, TextoCatalogo.Tipo.NOMBRE);
        boolean duplicado = id == null
                ? extraRepository.existsByNombreIgnoreCase(nombre.trim())
                : extraRepository.existsByNombreIgnoreCaseAndIdNot(nombre.trim(), id);
        if (duplicado) {
            throw new ConflictException("Ya existe un extra con ese nombre. Si está inactivo, reactiva el registro existente.");
        }
    }

    public void eliminar(Long id) {
        Extra extra = extraRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Extra no encontrado"));
        extra.desactivar();
    }
}
