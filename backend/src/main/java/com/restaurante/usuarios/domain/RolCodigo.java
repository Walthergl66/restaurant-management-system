package com.restaurante.usuarios.domain;

/** Códigos de roles del sistema (columna {@code roles.codigo}). */
public final class RolCodigo {

    private RolCodigo() {
    }

    public static final String ADMIN = "ADMIN";
    public static final String MESERO = "MESERO";
    public static final String COCINA = "COCINA";
    public static final String CAJERO = "CAJERO";
    public static final String CLIENTE = "CLIENTE";
}