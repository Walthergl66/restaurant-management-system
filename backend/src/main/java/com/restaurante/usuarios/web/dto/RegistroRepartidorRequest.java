package com.restaurante.usuarios.web.dto;

import com.restaurante.shared.validation.CelularValido;
import com.restaurante.shared.validation.PasswordSegura;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Auto-registro público de un repartidor (rol DELIVERY): crea el usuario con
 * su celular de contacto, pendiente de verificar el correo. El correo es el
 * nombre de usuario (login).
 *
 * <p>A diferencia del cliente, el repartidor no necesita cédula ni perfil
 * comercial: solo una cuenta con la que entrar a la app.
 */
public record RegistroRepartidorRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato válido")
        @Size(max = 50, message = "El correo no puede superar 50 caracteres")
        String username,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
        String nombre,

        @NotBlank(message = "El celular es obligatorio")
        @CelularValido
        String celular,

        @NotBlank(message = "La contraseña es obligatoria")
        @PasswordSegura
        String password) {
}
