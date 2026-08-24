package com.niniyumi.personalagent.weeklyreport.domain;

import java.time.Instant;
import java.time.LocalDate;

public record WeeklyReport(
        Long id,
        long userId,
        LocalDate weekStartDate,
        String coreWork,
        String problems,
        String nextWeekPlan,
        String sourceFileName,
        Instant createdAt,
        Instant updatedAt) {
}
