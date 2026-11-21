package com.restaurante.mesas.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Mesa registrada en el catálogo del salón, ubicada en una zona del restaurante. */
@Entity
@Table(name = "mesas", schema = "catalogo")
public class Mesa extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private int numero;

    @Column(nullable = false)
    private int capacidad;

    /** Hay pocas zonas por restaurante: se carga junto con la mesa para no depender de una sesión abierta. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "zona_id", nullable = false)
    private Zona zona;

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

    public Mesa(int numero, int capacidad, Zona zona) {
        cambiarNumero(numero);
        cambiarCapacidad(capacidad);
        cambiarZona(zona);
    }

    public int getNumero() {
        return numero;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public Zona getZona() {
        return zona;
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

    public void cambiarZona(Zona zona) {
        if (zona == null) {
            throw new BusinessRuleException("La mesa debe estar en una zona");
        }
        this.zona = zona;
    }

    public void deshabilitar() {
        this.activo = false;
    }

    public void habilitar() {
        this.activo = true;
    }
}
