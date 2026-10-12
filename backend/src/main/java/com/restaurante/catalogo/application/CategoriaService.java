package com.restaurante.catalogo.application;

import com.restaurante.catalogo.domain.Categoria;
import com.restaurante.catalogo.domain.NombreCatalogo;
import com.restaurante.catalogo.infrastructure.CategoriaRepository;
import com.restaurante.catalogo.web.dto.CategoriaRequest;
import com.restaurante.catalogo.web.dto.HabilitacionCatalogoRequest;
import com.restaurante.catalogo.web.dto.CategoriaResponse;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.restaurante.catalogo.web.dto.OrdenCategoriasRequest;
import com.restaurante.shared.domain.exception.BusinessRuleException;

/**
 * Administración de categorías del menú.
 */
@Service
@Transactional
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public CategoriaResponse crear(CategoriaRequest request) {
        validarNombreDisponible(request.nombre(), null);
        int maximo = categoriaRepository.findAll().stream().mapToInt(Categoria::getOrden).max().orElse(0);
        Categoria categoria = new Categoria(request.nombre(), maximo > 0 ? maximo + 1 : 0);
        categoria.cambiarDescripcion(request.descripcion());
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(boolean soloActivas) {
        List<Categoria> categorias = OrdenCategorias.ordenar(categoriaRepository.findAll());
        return categorias.stream()
                .filter(c -> !soloActivas || c.isActivo())
                .map(CategoriaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return CategoriaResponse.from(categoriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada")));
    }

    public CategoriaResponse actualizar(Long id, CategoriaRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        validarNombreDisponible(request.nombre(), id);
        categoria.cambiarNombre(request.nombre());
        categoria.cambiarDescripcion(request.descripcion());
        return CategoriaResponse.from(categoria);
    }

    public CategoriaResponse cambiarHabilitacion(Long id, HabilitacionCatalogoRequest request) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Registro de catálogo no encontrado"));
        if (request.habilitada()) {
            validarNombreDisponible(categoria.getNombre(), id);
            categoria.activar();
        } else {
            categoria.desactivar();
        }
        return CategoriaResponse.from(categoria);
    }

    private void validarNombreDisponible(String nombre, Long id) {
        nombre = NombreCatalogo.normalizar(nombre);
        boolean duplicado = id == null
                ? categoriaRepository.existsByNombreIgnoreCase(nombre.trim())
                : categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre.trim(), id);
        if (duplicado) {
            throw new ConflictException("Ya existe una categoría con ese nombre. Si está inactiva, reactiva el registro existente.");
        }
    }

    public List<CategoriaResponse> cambiarOrden(OrdenCategoriasRequest request) {
        List<Categoria> categorias = categoriaRepository.findAll();
        if (request.modo() == OrdenCategoriasRequest.Modo.ALFABETICO) {
            categorias.forEach(c -> c.cambiarOrden(0));
        } else {
            Map<Long, Categoria> porId = categorias.stream().collect(Collectors.toMap(Categoria::getId, Function.identity()));
            List<Long> ids = request.categoriaIds();
            if (ids.size() != categorias.size() || !new HashSet<>(ids).equals(porId.keySet())) {
                throw new BusinessRuleException("La lista de categorías cambió. Actualiza y vuelve a ordenar todas las categorías.");
            }
            for (int i = 0; i < ids.size(); i++) porId.get(ids.get(i)).cambiarOrden(i + 1);
        }
        return OrdenCategorias.ordenar(categorias).stream().map(CategoriaResponse::from).toList();
    }

    public void eliminar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        // Desactivación lógica: los productos históricos conservan la referencia.
        categoria.desactivar();
    }
}
