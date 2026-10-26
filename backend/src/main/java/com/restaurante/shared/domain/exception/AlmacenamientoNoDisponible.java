package com.restaurante.shared.domain.exception;

public class AlmacenamientoNoDisponible extends RuntimeException {
    public AlmacenamientoNoDisponible(Throwable cause) {
        super("El almacenamiento no está disponible. Intenta nuevamente en unos momentos.", cause);
    }
}
