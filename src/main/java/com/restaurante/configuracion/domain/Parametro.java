package com.restaurante.configuracion.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Parámetro de configuración del restaurante (clave/valor tipado).
 */
@Entity
@Table(name = "parametros")
public class Parametro extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 60)
    private String clave;

    @Column(nullable = false, length = 500)
    private String valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoParametro tipo = TipoParametro.TEXTO;

    @Column(length = 200)
    private String descripcion;

    protected Parametro() {
    }

    public Parametro(String clave, String valor, TipoParametro tipo, String descripcion) {
        cambiarClave(clave);
        this.valor = valor;
        this.tipo = tipo;
        this.descripcion = descripcion;
    }

    public String getClave() {
        return clave;
    }

    public String getValor() {
        return valor;
    }

    public TipoParametro getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void cambiarValor(String valor) {
        if (valor == null) {
            throw new BusinessRuleException("El valor del parámetro no puede ser nulo");
        }
        this.valor = valor.trim();
    }

    public BigDecimal valorComoBigDecimal() {
        if (tipo != TipoParametro.NUMERICO) {
            throw new BusinessRuleException("El parámetro '" + clave + "' no es numérico");
        }
        try {
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("El parámetro '" + clave + "' tiene un valor numérico inválido");
        }
    }

    public Boolean valorComoBoolean() {
        if (tipo != TipoParametro.BOOLEANO) {
            throw new BusinessRuleException("El parámetro '" + clave + "' no es booleano");
        }
        return Boolean.parseBoolean(valor);
    }

    private void cambiarClave(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new BusinessRuleException("La clave del parámetro no puede estar vacía");
        }
        this.clave = clave.trim();
    }
}