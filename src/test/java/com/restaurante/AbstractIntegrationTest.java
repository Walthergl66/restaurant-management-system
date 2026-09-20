package com.restaurante;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base de las pruebas de integración del sistema: PostgreSQL real en un único
 * contenedor compartido por toda la JVM. Compartirlo y no dejarlo en manos de
 * la extensión de JUnit evita que el contexto de Spring se reutilice con un
 * contenedor ya detenido entre clases de test.
 */
@SpringBootTest(properties = "spring.profiles.active=test")
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}