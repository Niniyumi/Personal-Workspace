package com.niniyumi.personalagent.weeklyreport.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReportRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WeeklyReportServiceTest {
    private static final long USER_ID = 42L;
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 24);
    private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

    @Mock
    private WeeklyReportRepository repository;

    private WeeklyReportService service;

    @BeforeEach
    void setUp() {
        service = new WeeklyReportService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createRejectsAWeekThatDoesNotStartOnMonday() {
        SaveWeeklyReportCommand command = command(MONDAY.plusDays(6), "核心工作", null, null);

        assertThatThrownBy(() -> service.create(USER_ID, command))
                .isInstanceOf(InvalidWeekStartException.class);
    }

    @Test
    void createRejectsAnExistingWeekForTheSameUser() {
        when(repository.findByUserIdAndWeekStartDate(USER_ID, MONDAY))
                .thenReturn(Optional.of(existingReport()));

        assertThatThrownBy(() -> service.create(USER_ID, command(MONDAY, "核心工作", null, null)))
                .isInstanceOf(WeeklyReportAlreadyExistsException.class);
    }

    @Test
    void createTrimsTextAndNormalizesBlankOptionalValues() {
        when(repository.findByUserIdAndWeekStartDate(USER_ID, MONDAY)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(USER_ID, command(MONDAY, "  完成登录  ", "   ", "  开发周报 "));

        ArgumentCaptor<WeeklyReport> saved = ArgumentCaptor.forClass(WeeklyReport.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().coreWork()).isEqualTo("完成登录");
        assertThat(saved.getValue().problems()).isNull();
        assertThat(saved.getValue().nextWeekPlan()).isEqualTo("开发周报");
        assertThat(saved.getValue().createdAt()).isEqualTo(NOW);
    }

    @Test
    void updateLoadsTheReportWithItsOwner() {
        when(repository.findByIdAndUserId(9L, USER_ID)).thenReturn(Optional.of(existingReport()));
        when(repository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(USER_ID, 9L, command(MONDAY, "更新后的工作", null, null));

        verify(repository).findByIdAndUserId(9L, USER_ID);
    }

    @Test
    void listCalculatesTheSelectedMonthRange() {
        service.list(USER_ID, 2026, 8);

        verify(repository).findByUserIdAndWeekStartDateBetween(
                USER_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
    }

    private SaveWeeklyReportCommand command(
            LocalDate weekStartDate, String coreWork, String problems, String nextWeekPlan) {
        return new SaveWeeklyReportCommand(
                weekStartDate, coreWork, problems, nextWeekPlan, "week-34.docx");
    }

    private WeeklyReport existingReport() {
        return new WeeklyReport(9L, USER_ID, MONDAY, "原工作", null, null, null,
                Instant.parse("2026-08-24T08:00:00Z"), Instant.parse("2026-08-24T09:00:00Z"));
    }
}
