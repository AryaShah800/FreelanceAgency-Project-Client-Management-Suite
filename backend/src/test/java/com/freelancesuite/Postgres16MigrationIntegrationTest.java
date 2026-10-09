package com.freelancesuite;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs in CI against an isolated PostgreSQL 16 service. Starting the production
 * Spring profile applies Flyway and Hibernate validates every mapped entity.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles({"prod", "migration-test"})
@EnabledIfEnvironmentVariable(named = "POSTGRES_MIGRATION_TEST", matches = "true")
class Postgres16MigrationIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void migrationRunsAndProductionEntitiesValidateAgainstPostgres16() {
        String serverVersion = jdbcTemplate.queryForObject("SHOW server_version", String.class);
        assertTrue(serverVersion != null && serverVersion.startsWith("16."),
                () -> "This CI migration test must run against PostgreSQL 16; connected to " + serverVersion);
        flyway.validate();
        assertTrue(flyway.info().current() != null
                        && "2".equals(flyway.info().current().getVersion().getVersion()),
                "Flyway V2 must be applied to the clean PostgreSQL 16 database.");
        assertTableExists("agencies");
        assertTableExists("app_users");
        assertTableExists("proposals");
        assertTableExists("invoices");
        assertTableExists("contracts");
        assertColumnExists("proposals", "client_email");
    }

    private void assertTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() AND table_name = ?",
                Integer.class,
                tableName);
        assertTrue(count != null && count == 1, "Expected migrated table " + tableName);
    }

    private void assertColumnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?",
                Integer.class, tableName, columnName);
        assertTrue(count != null && count == 1,
                () -> "Expected migrated column " + tableName + "." + columnName);
    }
}
