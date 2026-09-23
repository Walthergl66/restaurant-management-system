package com.restaurante.clientes.domain;

import com.restaurante.shared.domain.Money;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** RF-40 a RF-45 en UNA sola clase de transiciones (RF-07/RF-15/16/RF-22/23
 *  RF-24/25/26/27, misma regla RF-44 - igual Pedido RF-13/14/15/16 y
 *  PedidoCliente RF-22..27 del modulo pedidos Fase 3):
 *
 *      BORRADOR   --confirmar RF-40/41 IDEMPOTENTE--> CONFIRMADO
 *      CONFIRMADO --barra RF-18/19/20 RF-24/25 tablet--> EN_PREPARACION
 *      EN_PREPARACION --tablet RF-25--> LISTO
 *      LISTO --cliente RF-27 RF-42 retira/domicilio RF-45--> RETIRADO
 *      BORRADOR --cancelar RF-26/27 RF-47--> ANULADO
 *
 *  RF-41: el total se CONGELA en la confirmacion (Money SPI RF-41 RF-09
 *  unica regla RF-22/23, nunca double RF-09 - Money from catalogo SPI
 *  RF-09/22/23/26/41 Menu RF-40/41 Menu movil RF-48).
 *
 *  RF-44: el tablet RF-24/25 del mesero marca EN_PREPARACION/LISTO desde
 *  la tablet RF-14/15/16 (tablet mesero RF-44 tablet cocina RF-24/25).
 */
public class PedidoClienteTablet {

    private enum Estado { BORRADOR, CONFIRMADO, EN_PREPARACION, LISTO, RETIRADO, ANULADO }

    private final Long id;
    private final String codigo;
    private final Long clienteId;
    private final String idempotencyKey;
    private final List<LineaPedidoClienteTabletLinea> lineas = new ArrayList<>();
    private Estado estado = Estado.BORRADOR;
    private Money totalCongelado = Money.ZERO;
    private long version;

    public PedidoClienteTablet(Long id, String codigo, Long clienteId, String idempotencyKey) {
        this.id = id;
        this.codigo = codigo;
        this.clienteId = clienteId;
        this.idempotencyKey = idempotencyKey;
    }

    public void agregarLinea(LineaPedidoClienteTabletLinea linea) {
        exigir(Estado.BORRADOR, "RF-40 agregar al carrito");
        lineas.add(linea);
    }

    /** RF-41 confirmar idempotente: misma idempotencyKey => reintento 200
     *  (RF-24/25); clave distinta => 409 (RF-41/RF-26). Congela el total
     *  con Money SPI (RF-41 RF-09). Aplica a CUALQUIER estado no-BORRADOR
     *  como en Pedido confirmar (fix b20ace8), no solo CONFIRMADO. */
    public boolean confirmar(String claveReintento) {
        if (estado != Estado.BORRADOR) {
            if (claveReintento == null || !this.idempotencyKey.equals(claveReintento)) {
                throw new IllegalStateException("409 RF-41: clave idempotencia distinta (RF-24/25)");
            }
            return false; // reintento idempotente con misma clave = 200 (cualquier estado avanzado)
        }
        exigir(Estado.BORRADOR, "RF-41 confirmar");
        if (lineas.isEmpty()) {
            throw new IllegalStateException("RF-41: carrito vacio, no se confirma");
        }
        Money total = Money.ZERO;
        for (LineaPedidoClienteTabletLinea l : lineas) {
            total = total.add(l.subtotal());
        }
        this.totalCongelado = total;
        this.estado = Estado.CONFIRMADO;
        this.version++;
        return true;
    }

    /** RF-44 tablet del mesero RF-24: CONFIRMADO -> EN_PREPARACION. */
    public void marcarEnPreparacion() {
        exigir(Estado.CONFIRMADO, "RF-44 tablet EN_PREPARACION");
        this.estado = Estado.EN_PREPARACION;
        this.version++;
    }

    /** RF-44 tablet del mesero RF-25: EN_PREPARACION -> LISTO. */
    public void marcarListo() {
        exigir(Estado.EN_PREPARACION, "RF-44 tablet LISTO");
        this.estado = Estado.LISTO;
        this.version++;
    }

    /** RF-27/RF-42/RF-45: LISTO -> RETIRADO (retira en local o lo llevan). */
    public void retirar() {
        exigir(Estado.LISTO, "RF-27/RF-42 retirar/delivery");
        this.estado = Estado.RETIRADO;
        this.version++;
    }

    /** RF-26/RF-27/RF-47: BORRADOR -> ANULADO. */
    public void anular() {
        exigir(Estado.BORRADOR, "RF-26/27 anular");
        this.estado = Estado.ANULADO;
        this.version++;
    }

    private void exigir(Estado desde, String accion) {
        if (estado != desde) {
            throw new IllegalStateException(accion + " solo desde " + desde
                    + " (RF-44, actual=" + estado + ")");
        }
    }

    public Long id() { return id; }
    public String codigo() { return codigo; }
    public Long clienteId() { return clienteId; }
    public String idempotencyKey() { return idempotencyKey; }
    public List<LineaPedidoClienteTabletLinea> lineas() { return Collections.unmodifiableList(lineas); }
    public Money totalCongelado() { return totalCongelado; }
    public String estado() { return estado.name(); }
    public long version() { return version; }
}
