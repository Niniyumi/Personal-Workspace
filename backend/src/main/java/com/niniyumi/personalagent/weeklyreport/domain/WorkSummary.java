package com.niniyumi.personalagent.weeklyreport.domain;

import java.time.Instant;
import java.time.LocalDate;

public record WorkSummary(
        Long id,
        long userId,
        SummaryPeriodType periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        String coreContent,
        String routineWork,
        int selfScore,
        Instant generatedAt,
        Instant createdAt,
        Instant updatedAt) {
}
