package com.restaurante.catalogo.web;

import com.restaurante.catalogo.application.MenuPublicoService;
import com.restaurante.catalogo.web.dto.MenuDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Menú público sin autenticación, pensado para el QR de la mesa y la web del
 * cliente (RF-04). Expuesto en http://{host}/api/v1/menu.
 */
@RestController
@RequestMapping("/api/v1/menu")
@Tag(name = "catalogo", description = "Menú público")
public class MenuController {

    private final MenuPublicoService menuPublicoService;

    public MenuController(MenuPublicoService menuPublicoService) {
        this.menuPublicoService = menuPublicoService;
    }

    @GetMapping
    @Operation(summary = "Menú público para QR: categorías activas con sus productos")
    public MenuDto menu() {
        return menuPublicoService.menu();
    }
}