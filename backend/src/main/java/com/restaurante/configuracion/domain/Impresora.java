package com.restaurante.configuracion.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Impresora térmica del local: destino de las órdenes de impresión (Fase 4).
 * {@code area} es el nombre del área de preparación a la que sirve, en forma
 * nominal para no acoplar este módulo con el catálogo.
 */
@Entity
@Table(name = "impresoras")
public class Impresora extends AuditableEntity {

    @Column(nullable = false, unique = true, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoImpresora tipo;

    @Column(length = 45)
    private String ip;

    @Column(nullable = false)
    private int puerto = 9100;

    @Column(length = 100)
    private String area;

    @Column(nullable = false)
    private boolean activo = true;

    protected Impresora() {
    }

    public Impresora(String nombre, TipoImpresora tipo) {
        cambiarNombre(nombre);
        this.tipo = tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoImpresora getTipo() {
        return tipo;
    }

    public String getIp() {
        return ip;
    }

    public int getPuerto() {
        return puerto;
    }

    public String getArea() {
        return area;
    }

    public boolean isActivo() {
        return activo;
    }

    public void cambiarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessRuleException("El nombre de la impresora no puede estar vacío");
        }
        this.nombre = nombre.trim();
    }

    public void cambiarTipo(TipoImpresora tipo) {
        if (tipo == null) {
            throw new BusinessRuleException("El tipo de impresora es obligatorio");
        }
        this.tipo = tipo;
    }

    public void cambiarIp(String ip) {
        this.ip = ip == null || ip.isBlank() ? null : ip.trim();
    }

    public void cambiarPuerto(int puerto) {
        if (puerto <= 0 || puerto > 65535) {
            throw new BusinessRuleException("El puerto debe estar entre 1 y 65535");
        }
        this.puerto = puerto;
    }

    public void cambiarArea(String area) {
        this.area = area == null || area.isBlank() ? null : area.trim();
    }

    public void activar() {
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }
}