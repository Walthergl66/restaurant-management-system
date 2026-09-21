package com.restaurante.facturacion.infrastructure;

import com.restaurante.facturacion.EmisorInterno;
import com.restaurante.facturacion.EmitirComprobante;
import com.restaurante.facturacion.domain.Comprobante;
import org.springframework.stereotype.Component;

/**
 * Implementación del emisor interno: genera el correlativo secuencial legible
 * (R-000001) a partir del secuencial global de la base de datos.
 */
@Component
public class EmisorInternoDefault implements EmisorInterno {

    public static final String SERIE = "R";

    @Override
    public Comprobante emitir(EmitirComprobante command, Long secuencial) {
        String correlativo = SERIE + "-" + String.format("%06d", secuencial);
        return new Comprobante(
                correlativo,
                secuencial,
                command.tipo(),
                command.cuentaId(),
                command.total(),
                command.clienteNombre(),
                command.clienteIdentificacion(),
                command.emitidoPor());
    }
}