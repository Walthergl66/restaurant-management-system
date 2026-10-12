package com.restaurante.mesas.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Mesa registrada en el catálogo del salón. */
@Entity
@Table(name = "mesas", schema = "catalogo")
public class Mesa extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private int numero;

    @Column(nullable = false)
    private int capacidad;

    @Column(nullable = false, length = 100)
    private String ubicacion = "SALON";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMesa estado = EstadoMesa.LIBRE;

    @Column(nullable = false)
    private boolean activo = true;

    /** Evita que dos cambios administrativos concurrentes se sobrescriban. */
    @Version
    @Column(nullable = false)
    private Long version;

    protected Mesa() {
    }

    public Mesa(int numero, int capacidad) {
        cambiarNumero(numero);
        cambiarCapacidad(capacidad);
    }

    public int getNumero() {
        return numero;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public EstadoMesa getEstado() {
        return estado;
    }

    public boolean isActivo() {
        return activo;
    }

    public void cambiarNumero(int numero) {
        if (numero <= 0) {
            throw new BusinessRuleException("El número de mesa debe ser mayor que cero");
        }
        this.numero = numero;
    }

    public void cambiarCapacidad(int capacidad) {
        if (capacidad < 1 || capacidad > 50) {
            throw new BusinessRuleException("La capacidad debe estar entre 1 y 50 personas");
        }
        this.capacidad = capacidad;
    }

    public void cambiarUbicacion(String ubicacion) {
        this.ubicacion = ubicacion == null || ubicacion.isBlank() ? "SALON" : ubicacion.trim();
    }

    public void deshabilitar() {
        this.activo = false;
    }

    public void habilitar() {
        this.activo = true;
    }
}
