package com.restaurante.usuarios.infrastructure.correo;

/**
 * Fallo al entregar un correo al proveedor: error de red, timeout o una
 * respuesta HTTP distinta de 2xx. La lanza el adaptador y la registra
 * {@code EnvioCorreos} sin tumbar la operación ya confirmada.
 */
public class EnvioCorreoException extends RuntimeException {

    public EnvioCorreoException(String mensaje) {
        super(mensaje);
    }

    public EnvioCorreoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
