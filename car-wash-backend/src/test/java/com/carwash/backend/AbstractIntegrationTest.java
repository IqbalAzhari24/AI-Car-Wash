package com.carwash.backend;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton Testcontainers pattern: the containers are started once in a static
 * initializer and never stopped by us (Testcontainers' Ryuk reaper cleans them up
 * when the JVM exits). Every subclass shares the same running containers.
 *
 * <p>Deliberately NOT using {@code @Testcontainers}/{@code @Container} — that JUnit5
 * extension manages container lifecycle per test class (stopping them in an
 * after-all hook), which, combined with Spring's test-context caching, could leave
 * a cached {@code ApplicationContext} pointing at a container another test class had
 * already stopped, causing {@code CannotCreateTransactionException: Connection
 * refused} in whichever class happened to reuse that stale context.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @SuppressWarnings("resource")
    private static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
