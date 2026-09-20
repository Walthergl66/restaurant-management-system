package com.restaurante;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base de las pruebas de integración del sistema: PostgreSQL real vía
 * Testcontainers y migraciones de Flyway aplicadas en cada ejecución.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}