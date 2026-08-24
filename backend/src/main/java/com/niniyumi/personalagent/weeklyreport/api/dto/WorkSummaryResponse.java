package com.niniyumi.personalagent.weeklyreport.api.dto;

import com.niniyumi.personalagent.weeklyreport.domain.SummaryPeriodType;
import com.niniyumi.personalagent.weeklyreport.domain.WorkSummary;
import java.time.Instant;
import java.time.LocalDate;

public record WorkSummaryResponse(
        long id,
        SummaryPeriodType periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        String coreContent,
        String routineWork,
        int selfScore,
        Instant generatedAt,
        Instant createdAt,
        Instant updatedAt) {

    public static WorkSummaryResponse from(WorkSummary summary) {
        return new WorkSummaryResponse(
                summary.id(), summary.periodType(), summary.periodStart(), summary.periodEnd(),
                summary.coreContent(), summary.routineWork(), summary.selfScore(),
                summary.generatedAt(), summary.createdAt(), summary.updatedAt());
    }
}
