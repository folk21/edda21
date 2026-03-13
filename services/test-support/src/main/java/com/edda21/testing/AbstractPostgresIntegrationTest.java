package com.edda21.testing;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that require a real PostgreSQL instance.
 *
 * <p>This class starts a shared PostgreSQL container using Testcontainers and
 * exposes its connection properties to the Spring Boot test context via
 * {@code @DynamicPropertySource}.
 *
 * <p>Extend this class from any test that needs:
 * <p>- Flyway migrations to run against a real PostgreSQL database;
 * <p>- repositories and services to operate on a real schema instead of an in-memory database;
 * <p>- a single shared database container reused across multiple test classes to speed up the suite.
 *
 * <p>The container lifecycle is managed by the JUnit Jupiter Testcontainers extension
 * and the database is started once per test run.
 */
@Testcontainers
@SpringBootTest
public abstract class AbstractPostgresIntegrationTest {

  @Container
  @SuppressWarnings("resource")
  protected static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("edda21")
          .withUsername("postgres")
          .withPassword("postgres");

  @DynamicPropertySource
  static void registerPostgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }
}
