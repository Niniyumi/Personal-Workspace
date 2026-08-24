package com.niniyumi.personalagent.weeklyreport.application;

import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReportRepository;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WeeklyReportService {
    private final WeeklyReportRepository repository;
    private final Clock clock;

    public WeeklyReportService(WeeklyReportRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public WeeklyReport create(long userId, SaveWeeklyReportCommand command) {
        validateWeekStart(command.weekStartDate());
        if (repository.findByUserIdAndWeekStartDate(userId, command.weekStartDate()).isPresent()) {
            throw new WeeklyReportAlreadyExistsException();
        }

        Instant now = clock.instant();
        WeeklyReport report = new WeeklyReport(
                null,
                userId,
                command.weekStartDate(),
                command.coreWork().trim(),
                normalizeOptional(command.problems()),
                normalizeOptional(command.nextWeekPlan()),
                normalizeOptional(command.sourceFileName()),
                now,
                now);
        return repository.save(report);
    }

    public WeeklyReport update(long userId, long reportId, SaveWeeklyReportCommand command) {
        validateWeekStart(command.weekStartDate());
        WeeklyReport existing = repository.findByIdAndUserId(reportId, userId)
                .orElseThrow(WeeklyReportNotFoundException::new);

        if (!existing.weekStartDate().equals(command.weekStartDate())
                && repository.findByUserIdAndWeekStartDate(userId, command.weekStartDate()).isPresent()) {
            throw new WeeklyReportAlreadyExistsException();
        }

        WeeklyReport report = new WeeklyReport(
                existing.id(),
                userId,
                command.weekStartDate(),
                command.coreWork().trim(),
                normalizeOptional(command.problems()),
                normalizeOptional(command.nextWeekPlan()),
                normalizeOptional(command.sourceFileName()),
                existing.createdAt(),
                clock.instant());
        return repository.update(report);
    }

    public WeeklyReport get(long userId, long reportId) {
        return repository.findByIdAndUserId(reportId, userId)
                .orElseThrow(WeeklyReportNotFoundException::new);
    }

    public List<WeeklyReport> list(long userId, int year, int month) {
        YearMonth selectedMonth = YearMonth.of(year, month);
        return repository.findByUserIdAndWeekStartDateBetween(
                userId, selectedMonth.atDay(1), selectedMonth.atEndOfMonth());
    }

    private void validateWeekStart(LocalDate weekStartDate) {
        if (weekStartDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new InvalidWeekStartException();
        }
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
