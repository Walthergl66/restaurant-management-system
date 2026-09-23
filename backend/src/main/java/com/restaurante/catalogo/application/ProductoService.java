package com.restaurante.catalogo.application;

import com.restaurante.catalogo.Catalogo;
import com.restaurante.catalogo.ExtraParaPedido;
import com.restaurante.catalogo.ProductoParaPedido;
import com.restaurante.catalogo.domain.Area;
import com.restaurante.catalogo.domain.Categoria;
import com.restaurante.catalogo.domain.Extra;
import com.restaurante.catalogo.domain.Producto;
import com.restaurante.catalogo.infrastructure.AreaRepository;
import com.restaurante.catalogo.infrastructure.CategoriaRepository;
import com.restaurante.catalogo.infrastructure.ExtraRepository;
import com.restaurante.catalogo.infrastructure.ProductoRepository;
import com.restaurante.catalogo.web.dto.ProductoRequest;
import com.restaurante.catalogo.web.dto.ProductoResponse;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.domain.exception.ConflictException;
import com.restaurante.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Administración de productos del catálogo. También implementa {@link Catalogo},
 * la API pública que consumen otros módulos.
 */
@Service
@Transactional
public class ProductoService implements Catalogo {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AreaRepository areaRepository;
    private final ExtraRepository extraRepository;

    public ProductoService(ProductoRepository productoRepository,
                           CategoriaRepository categoriaRepository,
                           AreaRepository areaRepository,
                           ExtraRepository extraRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.areaRepository = areaRepository;
        this.extraRepository = extraRepository;
    }

    public ProductoResponse crear(ProductoRequest request) {
        if (productoRepository.existsByNombreIgnoreCase(request.nombre())) {
            throw new ConflictException("Ya existe un producto llamado '" + request.nombre() + "'");
        }
        Producto producto = new Producto(request.nombre(), Money.of(request.precio()));
        aplicarCampos(producto, request);
        return ProductoResponse.from(productoRepository.save(producto));
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findAll().stream()
                .map(ProductoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.from(cargar(id));
    }

    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = cargar(id);
        producto.cambiarNombre(request.nombre());
        aplicarCampos(producto, request);
        return ProductoResponse.from(producto);
    }

    public void eliminar(Long id) {
        Producto producto = cargar(id);
        // Desactivación lógica: los pedidos futuros conservan la referencia.
        producto.desactivar();
    }

    private void aplicarCampos(Producto producto, ProductoRequest request) {
        producto.cambiarDescripcion(request.descripcion());
        producto.cambiarImagenUrl(request.imagenUrl());
        producto.cambiarPrecio(Money.of(request.precio()));
        producto.cambiarCategoria(cargarCategoria(request.categoriaId()));
        producto.cambiarArea(cargarArea(request.areaId()));
        producto.reemplazarExtras(cargarExtras(request.extraIds()));
        producto.reemplazarIngredientes(request.ingredientes());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductoParaPedido> productoParaPedido(Long productoId) {
        return productoRepository.findById(productoId)
                .map(p -> new ProductoParaPedido(
                        p.getId(),
                        p.getNombre(),
                        p.getPrecio(),
                        p.getArea() == null ? null : p.getArea().getId(),
                        p.getArea() == null ? null : p.getArea().getNombre(),
                        p.isActivo()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExtraParaPedido> extraParaPedido(Long extraId) {
        return extraRepository.findById(extraId)
                .map(e -> new ExtraParaPedido(e.getId(), e.getNombre(), e.getPrecio(), e.isActivo()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoParaPedido> productosActivos() {
        return productoRepository.findByActivoTrue().stream()
                .map(p -> new ProductoParaPedido(
                        p.getId(),
                        p.getNombre(),
                        p.getPrecio(),
                        p.getArea() == null ? null : p.getArea().getId(),
                        p.getArea() == null ? null : p.getArea().getNombre(),
                        p.isActivo()))
                .toList();
    }

    private Categoria cargarCategoria(Long id) {
        if (id == null) {
            return null;
        }
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada: " + id));
        if (!categoria.isActivo()) {
            throw new BusinessRuleException("La categoría '" + categoria.getNombre() + "' está desactivada");
        }
        return categoria;
    }

    private Area cargarArea(Long id) {
        if (id == null) {
            return null;
        }
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Área no encontrada: " + id));
        if (!area.isActivo()) {
            throw new BusinessRuleException("El área '" + area.getNombre() + "' está desactivada");
        }
        return area;
    }

    private Set<Extra> cargarExtras(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        List<Extra> extras = extraRepository.findAllById(ids);
        if (extras.size() != ids.size()) {
            throw new NotFoundException("Uno o más extras no existen");
        }
        extras.stream().filter(e -> !e.isActivo()).findFirst().ifPresent(e -> {
            throw new BusinessRuleException("El extra '" + e.getNombre() + "' está desactivado");
        });
        return new HashSet<>(extras);
    }

    private Producto cargar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado"));
    }
}