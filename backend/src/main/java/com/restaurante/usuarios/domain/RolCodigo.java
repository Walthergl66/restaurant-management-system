package com.restaurante.usuarios.domain;

import java.util.Set;

/** Códigos de roles del sistema (columna {@code roles.codigo}). */
public final class RolCodigo {

    private RolCodigo() {
    }

    public static final String ADMIN = "ADMIN";
    public static final String MESERO = "MESERO";
    public static final String COCINA = "COCINA";
    public static final String DELIVERY = "DELIVERY";
    public static final String CLIENTE = "CLIENTE";

    /**
     * Roles cuyas funcionalidades puede ajustar el administrador por usuario
     * (seguridad.usuarios_permisos). ADMIN y CLIENTE quedan fijos: ADMIN con
     * todos los permisos y CLIENTE con los suyos de la app QR.
     */
    private static final Set<String> CONFIGURABLES = Set.of(MESERO, COCINA, DELIVERY);

    /** Indica si el rol admite permisos explícitos por usuario. */
    public static boolean esConfigurable(String codigo) {
        return CONFIGURABLES.contains(codigo);
    }
}