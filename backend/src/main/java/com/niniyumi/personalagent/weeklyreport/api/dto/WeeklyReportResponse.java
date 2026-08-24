package com.niniyumi.personalagent.weeklyreport.api.dto;

import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import java.time.Instant;
import java.time.LocalDate;

public record WeeklyReportResponse(
        Long id,
        LocalDate weekStartDate,
        String coreWork,
        String problems,
        String nextWeekPlan,
        String sourceFileName,
        Instant createdAt,
        Instant updatedAt) {

    public static WeeklyReportResponse from(WeeklyReport report) {
        return new WeeklyReportResponse(
                report.id(), report.weekStartDate(), report.coreWork(), report.problems(),
                report.nextWeekPlan(), report.sourceFileName(), report.createdAt(), report.updatedAt());
    }
}
