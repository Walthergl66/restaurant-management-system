package com.restaurante.catalogo.application;

import com.restaurante.catalogo.domain.Categoria;
import com.restaurante.catalogo.infrastructure.CategoriaRepository;
import com.restaurante.catalogo.web.dto.CategoriaRequest;
import com.restaurante.catalogo.web.dto.CategoriaResponse;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        if (categoriaRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ConflictException("Ya existe una categoría llamada '" + request.nombre() + "'");
        }
        Categoria categoria = new Categoria(request.nombre(), request.orden());
        categoria.cambiarDescripcion(request.descripcion());
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(boolean soloActivas) {
        List<Categoria> categorias = categoriaRepository.findAll().stream()
                .sorted((a, b) -> {
                    int porOrden = Integer.compare(a.getOrden(), b.getOrden());
                    return porOrden != 0 ? porOrden : a.getNombre().compareToIgnoreCase(b.getNombre());
                })
                .toList();
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
        categoria.cambiarNombre(request.nombre());
        categoria.cambiarDescripcion(request.descripcion());
        categoria.cambiarOrden(request.orden());
        return CategoriaResponse.from(categoria);
    }

    public void eliminar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        // Desactivación lógica: los productos históricos conservan la referencia.
        categoria.desactivar();
    }
}