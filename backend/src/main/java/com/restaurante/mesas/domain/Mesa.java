package com.restaurante.mesas.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Mesa del salón. Su estado cambia en este único lugar: los pedidos (Fase 3)
 * la ocupan o liberan llamando a {@link #ocupar()} y {@link #liberar()}.
 */
@Entity
@Table(name = "mesas")
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

    public boolean isLibre() {
        return activo && estado == EstadoMesa.LIBRE;
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

    /**
     * Ocupa la mesa: solo se puede desde LIBRE o RESERVADA.
     */
    public void ocupar() {
        if (estado == EstadoMesa.INACTIVA || !activo) {
            throw new BusinessRuleException("La mesa está inactiva");
        }
        if (estado == EstadoMesa.OCUPADA) {
            throw new BusinessRuleException("La mesa ya está ocupada");
        }
        this.estado = EstadoMesa.OCUPADA;
        this.activo = true;
    }

    /**
     * Libera la mesa: aplica desde cualquier estado activo.
     */
    public void liberar() {
        if (!activo) {
            throw new BusinessRuleException("La mesa está inactiva");
        }
        if (estado == EstadoMesa.LIBRE) {
            return;
        }
        this.estado = EstadoMesa.LIBRE;
    }

    /**
     * Reserva la mesa: solo desde LIBRE.
     */
    public void reservar() {
        if (estado == EstadoMesa.INACTIVA || !activo) {
            throw new BusinessRuleException("La mesa está inactiva");
        }
        if (estado != EstadoMesa.LIBRE) {
            throw new BusinessRuleException("Solo se puede reservar una mesa libre");
        }
        this.estado = EstadoMesa.RESERVADA;
    }

    public void desactivar() {
        this.estado = EstadoMesa.INACTIVA;
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
        if (estado == EstadoMesa.INACTIVA) {
            this.estado = EstadoMesa.LIBRE;
        }
    }
}