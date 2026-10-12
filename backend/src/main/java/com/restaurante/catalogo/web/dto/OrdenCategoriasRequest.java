package com.restaurante.catalogo.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrdenCategoriasRequest(
        @NotNull Modo modo,
        @NotNull List<@NotNull Long> categoriaIds) {
    public enum Modo { ALFABETICO, PERSONALIZADO }
}
