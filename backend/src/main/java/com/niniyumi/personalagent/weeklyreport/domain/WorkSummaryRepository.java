package com.niniyumi.personalagent.weeklyreport.domain;

import java.time.LocalDate;
import java.util.Optional;

public interface WorkSummaryRepository {
    WorkSummary save(WorkSummary summary);

    WorkSummary update(WorkSummary summary);

    Optional<WorkSummary> findByUserIdAndPeriod(
            long userId, SummaryPeriodType type, LocalDate start, LocalDate end);

    Optional<WorkSummary> findByIdAndUserId(long id, long userId);
}
