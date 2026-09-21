package com.restaurante;

import org.junit.jupiter.api.Test;

/**
 * Verifica que el contexto de Spring arranque contra un PostgreSQL real
 * (Testcontainers) con las migraciones de Flyway aplicadas.
 */
class RestaurantApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // El arranque del contexto ya valida datasource, Flyway y Hibernate
    }
}