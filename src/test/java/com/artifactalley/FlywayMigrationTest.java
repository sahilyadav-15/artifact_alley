package com.artifactalley;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.WebApplicationType;

import java.nio.file.Files;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationTest {
    @Test
    void migratesEmptySchemaAndSecondRunPreservesData() throws Exception {
        String url = "jdbc:h2:mem:flyway-repeat;DB_CLOSE_DELAY=-1";
        Flyway flyway = Flyway.configure().dataSource(url, "sa", "").locations("classpath:db/migration").load();

        assertEquals(1, flyway.migrate().migrationsExecuted);
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var insert = connection.prepareStatement("INSERT INTO app_users(name,email,password_hash,role) VALUES (?,?,?,?)")) {
            insert.setString(1, "Existing User");
            insert.setString(2, "existing@example.com");
            insert.setString(3, "hash");
            insert.setString(4, "BIDDER");
            assertEquals(1, insert.executeUpdate());
        }

        assertEquals(0, flyway.migrate().migrationsExecuted);
        assertEquals("1", flyway.info().current().getVersion().toString());
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var result = connection.createStatement().executeQuery("SELECT COUNT(*) FROM app_users WHERE email='existing@example.com'")) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            assertTrue(indexExists(connection, "ARTIFACTS", "IDX_ARTIFACT_STATUS_CLOSES"));
            assertTrue(indexExists(connection, "BIDS", "IDX_BID_ARTIFACT_PLACED"));
            assertThrows(java.sql.SQLException.class, () -> connection.createStatement().executeUpdate(
                    "INSERT INTO bids(artifact_id,bidder_id,amount,placed_at) VALUES(999,999,10,CURRENT_TIMESTAMP)"));
            assertThrows(java.sql.SQLException.class, () -> connection.createStatement().executeUpdate(
                    "INSERT INTO app_users(name,email,password_hash,role) VALUES('Bad','bad@example.com','hash','ROOT')"));
        }
    }

    @Test
    void changedAppliedMigrationFailsValidation() throws Exception {
        var directory = Files.createTempDirectory("flyway-checksum");
        var migration = directory.resolve("V1__schema.sql");
        Files.writeString(migration, "CREATE TABLE sample(id INTEGER PRIMARY KEY);\n");
        String url = "jdbc:h2:mem:flyway-checksum;DB_CLOSE_DELAY=-1";
        Flyway initial = Flyway.configure().dataSource(url, "sa", "").locations("filesystem:" + directory).load();
        initial.migrate();

        Files.writeString(migration, "CREATE TABLE sample(id BIGINT PRIMARY KEY);\n");
        Flyway changed = Flyway.configure().dataSource(url, "sa", "").locations("filesystem:" + directory).load();
        assertThrows(FlywayException.class, changed::validate);
    }

    @Test
    void applicationFailsWhenRequiredSchemaIsMissing() {
        String database = "jdbc:h2:mem:missing-schema;DB_CLOSE_DELAY=-1";
        assertThrows(Exception.class, () -> new SpringApplicationBuilder(ArtifactAlleyApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.profiles.active=local", "--spring.flyway.enabled=false",
                        "--spring.jpa.hibernate.ddl-auto=validate", "--spring.datasource.url=" + database,
                        "--spring.datasource.username=sa", "--spring.datasource.password=",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--artifactalley.initial-admin-password=test-admin-password"));
    }

    private boolean indexExists(java.sql.Connection connection, String table, String name) throws Exception {
        try (var indexes = connection.getMetaData().getIndexInfo(null, "PUBLIC", table, false, false)) {
            while (indexes.next()) if (name.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return true;
            return false;
        }
    }
}
