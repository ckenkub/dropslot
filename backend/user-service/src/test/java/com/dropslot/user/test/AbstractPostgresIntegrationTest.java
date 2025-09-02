package com.dropslot.user.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Shared Postgres Testcontainers setup for integration tests. */
public abstract class AbstractPostgresIntegrationTest {

  protected static final PostgreSQLContainer<?> PG =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
          .withDatabaseName("it_db")
          .withUsername("it")
          .withPassword("it");

  static {
    // ensure container is registered with Testcontainers lifecycle
    PG.start();
  }

  @DynamicPropertySource
  static void registerPgProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", PG::getJdbcUrl);
    registry.add("spring.datasource.username", PG::getUsername);
    registry.add("spring.datasource.password", PG::getPassword);
  }
}
