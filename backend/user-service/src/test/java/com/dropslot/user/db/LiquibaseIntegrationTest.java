package com.dropslot.user.db;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.sql.Connection;
import java.sql.DriverManager;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Quick integration test that starts a Postgres container and runs Liquibase against the project's
 * master changelog. This helps catch SQL changeset problems in CI.
 */
public class LiquibaseIntegrationTest {

  @Test
  public void liquibaseAppliesAllChangesets() {
    assertDoesNotThrow(
        () -> {
          DockerImageName image = DockerImageName.parse("postgres:16-alpine");
          try (PostgreSQLContainer<?> pg =
              new PostgreSQLContainer<>(image)
                  .withDatabaseName("test_db")
                  .withUsername("test")
                  .withPassword("test")) {
            pg.start();

            String jdbcUrl = pg.getJdbcUrl();
            String username = pg.getUsername();
            String password = pg.getPassword();

            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
              Database database =
                  DatabaseFactory.getInstance()
                      .findCorrectDatabaseImplementation(new JdbcConnection(conn));

              // changelog path relative to classpath
              String changeLog = "db/changelog/db.changelog-master.xml";

              try (Liquibase liquibase =
                  new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database)) {
                liquibase.update(new Contexts(), new LabelExpression());
              }
            }
          }
        });
  }
}
