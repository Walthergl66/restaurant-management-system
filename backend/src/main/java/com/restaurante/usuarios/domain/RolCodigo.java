package com.restaurante.usuarios.domain;

/** Códigos de roles del sistema (columna {@code roles.codigo}). */
public final class RolCodigo {

    private RolCodigo() {
    }

    public static final String ADMIN = "ADMIN";
    public static final String EMPLEADO = "EMPLEADO";
    public static final String DELIVERY = "DELIVERY";
    public static final String CLIENTE = "CLIENTE";
}