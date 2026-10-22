package com.restaurante.usuarios.domain;

/**
 * Códigos de roles del sistema (columna {@code roles.codigo}).
 *
 * <p>Los roles vigentes son cuatro categorías con sus subroles:
 * {@code ADMIN} (sin subroles), {@code EMPLEADO} (subroles {@code MESERO} y
 * {@code CAJERO}), {@code CLIENTE} y {@code DELIVERY}.
 *
 * <p>Lo que el administrador puede hacer con cada rol (asignarlo, ajustarle
 * las funcionalidades) no vive en esta clase: son banderas del propio rol en
 * la base de datos ({@code roles.asignable_por_admin} y
 * {@code roles.configurable}), que la migración V15 sembró y que se leen de
 * {@link Rol}. Aquí solo viven los códigos que el código necesita nombrar.
 */
public final class RolCodigo {

    private RolCodigo() {
    }

    public static final String ADMIN = "ADMIN";
    public static final String MESERO = "MESERO";
    public static final String CAJERO = "CAJERO";
    public static final String DELIVERY = "DELIVERY";
    public static final String CLIENTE = "CLIENTE";
}
