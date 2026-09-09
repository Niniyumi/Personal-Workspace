package com.niniyumi.personalagent.weeklyreport.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyReportRepository {
    WeeklyReport save(WeeklyReport report);

    WeeklyReport update(WeeklyReport report);

    Optional<WeeklyReport> findByIdAndUserId(long id, long userId);

    Optional<WeeklyReport> findByUserIdAndWeekStartDate(long userId, LocalDate weekStartDate);

    List<WeeklyReport> findByUserIdAndWeekStartDateBetween(
            long userId, LocalDate startDate, LocalDate endDate);

    List<WeeklyReport> findAllByUserId(long userId);
}
