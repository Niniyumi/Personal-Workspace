package com.niniyumi.personalagent.weeklyreport.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class WeeklyReportMigrationContractTest {

    @Test
    void migrationDefinesOwnedWeeklyReportsAndWorkSummaries() throws IOException {
        String sql = Files.readString(
                Path.of("src/main/resources/db/migration/V2__create_weekly_report_tables.sql"));

        assertThat(sql)
                .contains("CREATE TABLE weekly_reports", "CREATE TABLE work_summaries")
                .contains("UNIQUE KEY uk_weekly_reports_user_week")
                .contains("CONSTRAINT fk_weekly_reports_user")
                .contains("CHECK (self_score BETWEEN 0 AND 100)");
    }
}
