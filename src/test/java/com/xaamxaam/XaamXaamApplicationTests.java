package com.xaamxaam;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifie simplement que le contexte Spring demarre correctement, avec une
 * vraie instance PostgreSQL ephemere (Testcontainers) plutot qu'une base
 * H2 en memoire, pour detecter tot les problemes specifiques a PostgreSQL.
 */
@SpringBootTest
@Testcontainers
class XaamXaamApplicationTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("xaamxaam_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configurerProprietes(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.claude.api-key", () -> "test-key");
        registry.add("app.jwt.secret", () -> "test-secret-key-with-at-least-256-bits-for-hmac-sha");
        registry.add("app.ocr.api-key", () -> "test-ocr-key");
    }

    @Test
    void contextLoads() {
        // Le test reussit si le contexte Spring demarre sans exception.
    }
}
