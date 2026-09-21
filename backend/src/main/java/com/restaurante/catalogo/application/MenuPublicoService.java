package com.restaurante.catalogo.application;

import com.restaurante.catalogo.domain.Categoria;
import com.restaurante.catalogo.domain.Producto;
import com.restaurante.catalogo.infrastructure.CategoriaRepository;
import com.restaurante.catalogo.infrastructure.ProductoRepository;
import com.restaurante.catalogo.web.dto.CategoriaMenuDto;
import com.restaurante.catalogo.web.dto.InfoRestauranteDto;
import com.restaurante.catalogo.web.dto.MenuDto;
import com.restaurante.catalogo.web.dto.ProductoMenuDto;
import com.restaurante.configuracion.Parametros;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Menú público que sirve el QR: solo categorías y productos activos,
 * sin autenticación.
 */
@Service
@Transactional(readOnly = true)
public class MenuPublicoService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final Parametros parametros;

    public MenuPublicoService(CategoriaRepository categoriaRepository,
                              ProductoRepository productoRepository,
                              Parametros parametros) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.parametros = parametros;
    }

    public MenuDto menu() {
        List<Categoria> categorias = categoriaRepository.findByActivoTrueOrderByOrdenAscNombreAsc();
        Map<Long, List<Producto>> porCategoria = agruparPorCategoria();

        List<CategoriaMenuDto> categoriasMenu = categorias.stream()
                .map(categoria -> CategoriaMenuDto.of(
                        categoria.getId(),
                        categoria.getNombre(),
                        porCategoria.getOrDefault(categoria.getId(), List.of()).stream()
                                .map(ProductoMenuDto::from)
                                .toList()))
                .toList();

        return new MenuDto(
                new InfoRestauranteDto(parametros.nombreRestaurante(), parametros.moneda()),
                categoriasMenu);
    }

    private Map<Long, List<Producto>> agruparPorCategoria() {
        Map<Long, List<Producto>> mapa = new LinkedHashMap<>();
        productoRepository.findByActivoTrue().stream()
                .filter(p -> p.getCategoria() != null)
                .forEach(p -> mapa.computeIfAbsent(p.getCategoria().getId(), k -> new java.util.ArrayList<>())
                        .add(p));
        return mapa;
    }
}