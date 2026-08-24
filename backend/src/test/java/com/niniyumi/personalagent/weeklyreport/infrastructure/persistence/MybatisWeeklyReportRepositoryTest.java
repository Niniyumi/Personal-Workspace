package com.niniyumi.personalagent.weeklyreport.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MybatisWeeklyReportRepositoryTest {
    @Mock
    private WeeklyReportMapper mapper;

    private MybatisWeeklyReportRepository repository;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), WeeklyReportRow.class);
        repository = new MybatisWeeklyReportRepository(mapper);
    }

    @Test
    void findByIdAlwaysIncludesTheOwner() {
        repository.findByIdAndUserId(9L, 42L);

        assertThat(capturedSelectOneQuery().getSqlSegment())
                .contains("id", "user_id");
    }

    @Test
    void listAlwaysIncludesOwnerDateRangeAndNewestUpdateFirst() {
        repository.findByUserIdAndWeekStartDateBetween(
                42L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        ArgumentCaptor<Wrapper<WeeklyReportRow>> query = wrapperCaptor();
        verify(mapper).selectList(query.capture());
        assertThat(query.getValue().getSqlSegment())
                .contains("user_id", "week_start_date", "ORDER BY updated_at DESC");
    }

    @Test
    void updateUsesBothReportIdAndOwnerId() {
        WeeklyReport report = new WeeklyReport(
                9L, 42L, LocalDate.of(2026, 8, 24), "完成登录", null, "开始周报",
                null, Instant.parse("2026-08-24T08:00:00Z"), Instant.parse("2026-08-24T09:00:00Z"));

        repository.update(report);

        ArgumentCaptor<Wrapper<WeeklyReportRow>> query = wrapperCaptor();
        verify(mapper).update(any(WeeklyReportRow.class), query.capture());
        assertThat(query.getValue().getSqlSegment())
                .contains("id", "user_id");
    }

    @Test
    void listMapsRowsBackToDomainObjects() {
        WeeklyReportRow row = WeeklyReportRow.fromDomain(new WeeklyReport(
                9L, 42L, LocalDate.of(2026, 8, 24), "完成登录", null, null,
                null, Instant.parse("2026-08-24T08:00:00Z"), Instant.parse("2026-08-24T09:00:00Z")));
        when(mapper.selectList(any())).thenReturn(List.of(row));

        List<WeeklyReport> reports = repository.findByUserIdAndWeekStartDateBetween(
                42L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

        assertThat(reports).extracting(WeeklyReport::id).containsExactly(9L);
    }

    private Wrapper<WeeklyReportRow> capturedSelectOneQuery() {
        ArgumentCaptor<Wrapper<WeeklyReportRow>> query = wrapperCaptor();
        verify(mapper).selectOne(query.capture());
        return query.getValue();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentCaptor<Wrapper<WeeklyReportRow>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }
}
