package com.restaurante.usuarios.web.dto;

import java.util.List;

/**
 * Categoría de roles con los roles que contiene.
 *
 * <p>La categoría no concede permisos: solo agrupa. Sirve para presentar los
 * roles agrupados en el panel (por ejemplo, EMPLEADO con sus subroles MESERO
 * y CAJERO).
 */
public record CategoriaRolDto(
        String codigo,
        String descripcion,
        List<RolDto> roles) {
}
