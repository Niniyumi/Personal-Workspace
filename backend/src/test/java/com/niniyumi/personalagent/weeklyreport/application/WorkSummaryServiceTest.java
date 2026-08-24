package com.niniyumi.personalagent.weeklyreport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.niniyumi.personalagent.weeklyreport.domain.SummaryPeriodType;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReportRepository;
import com.niniyumi.personalagent.weeklyreport.domain.WorkSummary;
import com.niniyumi.personalagent.weeklyreport.domain.WorkSummaryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkSummaryServiceTest {
    private static final long USER_ID = 42L;
    private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

    @Mock private WeeklyReportRepository weeklyRepository;
    @Mock private WorkSummaryRepository summaryRepository;
    @Mock private WeeklyReportAiService aiService;

    private WorkSummaryService service;

    @BeforeEach
    void setUp() {
        service = new WorkSummaryService(
                weeklyRepository, summaryRepository, aiService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void generatesTheExactQuarterRange() {
        WeeklyReport report = report();
        when(weeklyRepository.findByUserIdAndWeekStartDateBetween(
                USER_ID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(List.of(report));
        when(aiService.generateSummary(List.of(report)))
                .thenReturn(new GeneratedWorkSummary("核心成果", "日常维护", 88));
        when(summaryRepository.findByUserIdAndPeriod(
                USER_ID, SummaryPeriodType.QUARTER,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30)))
                .thenReturn(Optional.empty());
        when(summaryRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkSummary result = service.generateQuarter(USER_ID, 2026, 3);

        assertThat(result.periodStart()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(result.periodEnd()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(result.selfScore()).isEqualTo(88);
        verify(summaryRepository).save(org.mockito.ArgumentMatchers.any(WorkSummary.class));
    }

    @Test
    void generatesTheExactYearRange() {
        WeeklyReport report = report();
        when(weeklyRepository.findByUserIdAndWeekStartDateBetween(
                USER_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(List.of(report));
        when(aiService.generateSummary(List.of(report)))
                .thenReturn(new GeneratedWorkSummary("核心成果", "日常维护", 90));
        when(summaryRepository.findByUserIdAndPeriod(
                USER_ID, SummaryPeriodType.YEAR,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)))
                .thenReturn(Optional.empty());
        when(summaryRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkSummary result = service.generateYear(USER_ID, 2026);

        assertThat(result.periodType()).isEqualTo(SummaryPeriodType.YEAR);
        assertThat(result.periodEnd()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    void emptyPeriodStopsBeforeCallingTheProvider() {
        when(weeklyRepository.findByUserIdAndWeekStartDateBetween(
                USER_ID, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.generateQuarter(USER_ID, 2026, 1))
                .isInstanceOf(NoWeeklyReportsForPeriodException.class);

        verify(aiService, never()).generateSummary(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void regenerationUpdatesTheOwnedExistingSummary() {
        WeeklyReport report = report();
        WorkSummary existing = new WorkSummary(
                7L, USER_ID, SummaryPeriodType.YEAR,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                "旧核心", "旧日常", 70, NOW.minusSeconds(100), NOW.minusSeconds(200), NOW.minusSeconds(100));
        when(weeklyRepository.findByUserIdAndWeekStartDateBetween(
                USER_ID, existing.periodStart(), existing.periodEnd())).thenReturn(List.of(report));
        when(aiService.generateSummary(List.of(report)))
                .thenReturn(new GeneratedWorkSummary("新核心", "新日常", 92));
        when(summaryRepository.findByUserIdAndPeriod(
                USER_ID, SummaryPeriodType.YEAR, existing.periodStart(), existing.periodEnd()))
                .thenReturn(Optional.of(existing));
        when(summaryRepository.update(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        WorkSummary result = service.generateYear(USER_ID, 2026);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.createdAt()).isEqualTo(existing.createdAt());
        verify(summaryRepository).update(org.mockito.ArgumentMatchers.any(WorkSummary.class));
        verify(summaryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateRejectsAScoreOutsideZeroToOneHundred() {
        assertThatThrownBy(() -> service.update(USER_ID, 7L, "核心", "日常", 101))
                .isInstanceOf(IllegalArgumentException.class);
        verify(summaryRepository, never()).findByIdAndUserId(7L, USER_ID);
    }

    private WeeklyReport report() {
        return new WeeklyReport(9L, USER_ID, LocalDate.of(2026, 8, 24), "完成登录", null,
                "开发周报", null, NOW.minusSeconds(100), NOW);
    }
}
