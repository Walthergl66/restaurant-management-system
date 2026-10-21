package com.restaurante.usuarios.web.dto;

import java.util.List;

/**
 * Taxonomía de roles del sistema: las categorías con sus roles y la lista
 * plana de todos ellos.
 *
 * <p>La vista anidada ({@code categorias}) sirve para presentar los roles
 * agrupados y la vista plana ({@code roles}) para filtrar o elegir uno. Las
 * dos contienen los mismos roles: con cinco roles, la duplicación pesa más
 * que el ahorro de bytes.
 */
public record TaxonomiaRolesDto(
        List<CategoriaRolDto> categorias,
        List<RolDto> roles) {
}
