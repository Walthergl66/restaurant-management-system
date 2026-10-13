package com.restaurante.catalogo.web.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import com.restaurante.catalogo.application.OrdenCategoriasCommand;

public record OrdenCategoriasRequest(
        @NotNull OrdenCategoriasCommand.Modo modo,
        @NotNull List<@NotNull Long> categoriaIds) {
    public OrdenCategoriasCommand toCommand() {
        return new OrdenCategoriasCommand(modo, categoriaIds);
    }
}
