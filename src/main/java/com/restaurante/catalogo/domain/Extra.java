package com.restaurante.catalogo.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.Money;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import com.restaurante.shared.infrastructure.MoneyConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Adición que acompaña a un producto (topping) con su propio precio.
 */
@Entity
@Table(name = "extras")
public class Extra extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @Convert(converter = MoneyConverter.class)
    @Column(nullable = false, precision = 12, scale = 2)
    private Money precio = Money.ZERO;

    @Column(nullable = false)
    private boolean activo = true;

    protected Extra() {
    }

    public Extra(String nombre, Money precio) {
        cambiarNombre(nombre);
        cambiarPrecio(precio);
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Money getPrecio() {
        return precio;
    }

    public boolean isActivo() {
        return activo;
    }

    public void cambiarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessRuleException("El nombre del extra no puede estar vacío");
        }
        this.nombre = nombre.trim();
    }

    public void cambiarDescripcion(String descripcion) {
        this.descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
    }

    public void cambiarPrecio(Money precio) {
        if (precio == null || precio.isNegative()) {
            throw new BusinessRuleException("El precio del extra no puede ser negativo");
        }
        this.precio = precio;
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }
}