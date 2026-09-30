package com.restaurante;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Excluye {@link UserDetailsServiceAutoConfiguration}: la autenticación es JWT
 * stateless contra la base (ver AuthService), sin {@code UserDetailsService} ni
 * {@code AuthenticationManager}. Sin esta exclusión Spring Boot crea un usuario
 * en memoria con contraseña aleatoria que nadie puede usar.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableScheduling
public class RestaurantApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestaurantApplication.class, args);
    }
}