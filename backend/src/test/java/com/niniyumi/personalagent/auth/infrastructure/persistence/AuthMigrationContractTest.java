package com.niniyumi.personalagent.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AuthMigrationContractTest {

    @Test
    void migrationContainsRequiredTablesAndUniqueKeys() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V1__create_auth_tables.sql"));

        assertThat(sql).contains("CREATE TABLE users", "CREATE TABLE refresh_sessions");
        assertThat(sql).contains("UNIQUE KEY uk_users_username", "UNIQUE KEY uk_users_email");
        assertThat(sql).doesNotContain("123" + "456");
    }
}
