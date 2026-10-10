package com.restaurante.usuarios.web.dto;

/**
 * Resultado del auto-registro de un cliente (RF-45): la cuenta quedó creada
 * pendiente de verificar el correo. No incluye tokens: se emiten al verificar.
 */
public record RegistroPendienteResponse(String username, boolean emailVerificado, String mensaje) {
}
