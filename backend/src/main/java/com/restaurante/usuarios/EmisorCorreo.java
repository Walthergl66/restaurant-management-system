package com.restaurante.usuarios;

/**
 * Puerto de salida para el envío de correos transaccionales del módulo usuarios.
 * En este alcance no hay servidor SMTP: la implementación por defecto registra
 * el contenido en el log (entrega manual en desarrollo). En producción se
 * reemplaza por un adaptador SMTP/SaaS sin tocar la capa de aplicación.
 */
public interface EmisorCorreo {

    /** Envía al cliente el código de verificación de su correo (RF-45). */
    void enviarCodigoVerificacion(String correo, String codigo);
}
