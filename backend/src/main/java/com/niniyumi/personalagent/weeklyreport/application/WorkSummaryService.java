package com.niniyumi.personalagent.weeklyreport.application;

import com.niniyumi.personalagent.weeklyreport.domain.SummaryPeriodType;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReportRepository;
import com.niniyumi.personalagent.weeklyreport.domain.WorkSummary;
import com.niniyumi.personalagent.weeklyreport.domain.WorkSummaryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class WorkSummaryService {
    private final WeeklyReportRepository weeklyRepository;
    private final WorkSummaryRepository summaryRepository;
    private final WeeklyReportAiService aiService;
    private final Clock clock;

    public WorkSummaryService(
            WeeklyReportRepository weeklyRepository,
            WorkSummaryRepository summaryRepository,
            WeeklyReportAiService aiService,
            Clock clock) {
        this.weeklyRepository = weeklyRepository;
        this.summaryRepository = summaryRepository;
        this.aiService = aiService;
        this.clock = clock;
    }

    public WorkSummary generateQuarter(long userId, int year, int quarter) {
        if (quarter < 1 || quarter > 4) {
            throw new InvalidSummaryPeriodException();
        }
        int startMonth = (quarter - 1) * 3 + 1;
        LocalDate start = LocalDate.of(year, startMonth, 1);
        LocalDate end = start.plusMonths(2).withDayOfMonth(start.plusMonths(2).lengthOfMonth());
        return generate(userId, SummaryPeriodType.QUARTER, start, end);
    }

    public WorkSummary generateYear(long userId, int year) {
        return generate(userId, SummaryPeriodType.YEAR,
                LocalDate.of(year, Month.JANUARY, 1), LocalDate.of(year, Month.DECEMBER, 31));
    }

    public WorkSummary get(
            long userId, SummaryPeriodType type, LocalDate start, LocalDate end) {
        return summaryRepository.findByUserIdAndPeriod(userId, type, start, end)
                .orElseThrow(WorkSummaryNotFoundException::new);
    }

    public WorkSummary update(
            long userId, long summaryId, String coreContent, String routineWork, int selfScore) {
        validateContent(coreContent, routineWork, selfScore);
        WorkSummary existing = summaryRepository.findByIdAndUserId(summaryId, userId)
                .orElseThrow(WorkSummaryNotFoundException::new);
        WorkSummary updated = new WorkSummary(
                existing.id(), userId, existing.periodType(), existing.periodStart(), existing.periodEnd(),
                coreContent.trim(), routineWork.trim(), selfScore, existing.generatedAt(),
                existing.createdAt(), clock.instant());
        return summaryRepository.update(updated);
    }

    private WorkSummary generate(
            long userId, SummaryPeriodType type, LocalDate start, LocalDate end) {
        List<WeeklyReport> reports = weeklyRepository.findByUserIdAndWeekStartDateBetween(userId, start, end);
        if (reports.isEmpty()) {
            throw new NoWeeklyReportsForPeriodException();
        }

        // 先成功生成内容，再读取并覆盖旧记录，模型失败时数据库保持不变。
        GeneratedWorkSummary generated = aiService.generateSummary(reports);
        Instant now = clock.instant();
        return summaryRepository.findByUserIdAndPeriod(userId, type, start, end)
                .map(existing -> summaryRepository.update(new WorkSummary(
                        existing.id(), userId, type, start, end,
                        generated.coreContent(), generated.routineWork(), generated.selfScore(),
                        now, existing.createdAt(), now)))
                .orElseGet(() -> summaryRepository.save(new WorkSummary(
                        null, userId, type, start, end,
                        generated.coreContent(), generated.routineWork(), generated.selfScore(),
                        now, now, now)));
    }

    private void validateContent(String coreContent, String routineWork, int selfScore) {
        if (coreContent == null || coreContent.isBlank()
                || routineWork == null || routineWork.isBlank()) {
            throw new IllegalArgumentException("summary content is required");
        }
        if (selfScore < 0 || selfScore > 100) {
            throw new IllegalArgumentException("selfScore must be between 0 and 100");
        }
    }
}
