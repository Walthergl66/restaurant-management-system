package com.restaurante.comandas.domain;

import com.restaurante.shared.domain.AuditableEntity;
import com.restaurante.shared.domain.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Comanda de preparación de un área. Hay UNA por (pedido, área): la restricción
 * única en la base protege de duplicados aunque el evento se reintente.
 */
@Entity
@Table(name = "comandas")
public class Comanda extends AuditableEntity {

    @Column(name = "pedido_codigo", nullable = false, length = 40)
    private String pedidoCodigo;

    @Column(name = "numero_comanda", nullable = false)
    private int numeroComanda;

    @Column(name = "area_id", nullable = false)
    private Long areaId;

    @Column(name = "area_nombre", nullable = false, length = 80)
    private String areaNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoComanda tipo = TipoComanda.ORDEN;

    @Column(name = "anulacion_id")
    private Long anulacionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ComandaEstado estado = ComandaEstado.PENDIENTE;

    @OneToMany(mappedBy = "comanda", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden")
    private Set<ComandaLinea> lineas = new LinkedHashSet<>();

    protected Comanda() {
    }

    public Comanda(String pedidoCodigo, int numeroComanda, Long areaId, String areaNombre) {
        this(pedidoCodigo, numeroComanda, areaId, areaNombre, TipoComanda.ORDEN, null);
    }

    public Comanda(String pedidoCodigo, int numeroComanda, Long areaId, String areaNombre,
                   TipoComanda tipo, Long anulacionId) {
        this.pedidoCodigo = pedidoCodigo;
        this.numeroComanda = numeroComanda;
        this.areaId = areaId;
        this.areaNombre = areaNombre;
        this.tipo = tipo;
        this.anulacionId = anulacionId;
    }

    public void agregarLinea(ComandaLinea linea) {
        linea.agregarAComanda(this);
        lineas.add(linea);
    }

    public void marcarEnPreparacion() {
        if (estado != ComandaEstado.PENDIENTE && estado != ComandaEstado.EN_PREPARACION) {
            throw new BusinessRuleException(
                    "Una comanda " + estado.name().toLowerCase() + " no puede volver a preparación");
        }
        this.estado = ComandaEstado.EN_PREPARACION;
    }

    public void marcarListo() {
        if (estado != ComandaEstado.EN_PREPARACION && estado != ComandaEstado.LISTO) {
            throw new BusinessRuleException(
                    "Una comanda " + estado.name().toLowerCase() + " no puede marcarse lista");
        }
        this.estado = ComandaEstado.LISTO;
    }

    public String getPedidoCodigo() {
        return pedidoCodigo;
    }

    public int getNumeroComanda() {
        return numeroComanda;
    }

    public Long getAreaId() {
        return areaId;
    }

    public String getAreaNombre() {
        return areaNombre;
    }

    public ComandaEstado getEstado() {
        return estado;
    }

    public TipoComanda getTipo() {
        return tipo;
    }

    public Long getAnulacionId() {
        return anulacionId;
    }

    public List<ComandaLinea> getLineas() {
        return List.copyOf(lineas);
    }
}