package com.carwash.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Each subclass gets its own fresh Postgres/Redis containers (several tests, e.g.
 * {@code BookingServiceIntegrationTest}, rely on starting from a pristine, freshly
 * migrated+seeded database per test class).
 *
 * <p>{@code @DirtiesContext(classMode = AFTER_CLASS)} is required here: without it,
 * Spring's test-context cache can serve a later test class a cached
 * {@code ApplicationContext} that was built against an earlier class's container —
 * one {@code @Testcontainers} already stopped in its after-all hook once that
 * class finished. That stale reference surfaces as
 * {@code CannotCreateTransactionException: Connection refused} in whichever class
 * unluckily reuses the dead context. Forcing the context to be discarded after each
 * class guarantees every class builds its context against its own live container.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @SuppressWarnings("resource")
    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
