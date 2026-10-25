package com.restaurante.usuarios;

import java.time.Duration;

/**
 * Puerto de salida para el envío de correos transaccionales del módulo usuarios.
 * La implementación se elige por configuración: en desarrollo y pruebas el
 * contenido se registra en el log (entrega manual) y con
 * {@code app.correo.brevo.habilitado=true} sale por la API de Brevo. La capa de
 * aplicación no conoce al proveedor.
 */
public interface EmisorCorreo {

    /** Envía al cliente el código de verificación de su correo (RF-45). */
    void enviarCodigoVerificacion(String correo, String codigo);

    /**
     * Envía el token de un solo uso para restablecer la contraseña (RF-45),
     * indicando cuánto tiempo es válido.
     */
    void enviarRecuperacionPassword(String correo, String token, Duration vigencia);
}
