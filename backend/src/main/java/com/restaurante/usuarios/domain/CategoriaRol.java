package com.restaurante.usuarios.domain;

import com.restaurante.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Categoría de roles: agrupa los roles de primer nivel del sistema.
 *
 * <p>Categorías vigentes:
 * <ul>
 *   <li>{@code ADMIN}: administración del sistema (una sola cuenta).</li>
 *   <li>{@code EMPLEADO}: personal interno, con los subroles {@code MESERO}
 *       y {@code CAJERO}.</li>
 *   <li>{@code CLIENTE}: clientes de la app móvil.</li>
 *   <li>{@code DELIVERY}: repartidores.</li>
 * </ul>
 *
 * <p>La categoría no concede permisos por sí misma: solo organiza los roles
 * y permite agruparlos en el panel. Los permisos cuelgan del rol.
 */
@Entity
@Table(name = "categorias_roles", schema = "seguridad")
public class CategoriaRol extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(length = 120)
    private String descripcion;

    protected CategoriaRol() {
    }

    public CategoriaRol(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
