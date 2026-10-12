package com.restaurante.catalogo.application;

import java.util.List;
import java.util.Objects;

/** Entrada del caso de uso, independiente del transporte HTTP. */
public record OrdenCategoriasCommand(Modo modo, List<Long> categoriaIds) {
    public enum Modo { ALFABETICO, PERSONALIZADO }

    public OrdenCategoriasCommand {
        Objects.requireNonNull(modo, "El modo es obligatorio");
        categoriaIds = List.copyOf(categoriaIds);
    }
}
