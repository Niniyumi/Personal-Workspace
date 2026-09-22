package com.niniyumi.personalagent.weeklyreport.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MybatisWeeklyReportRepositoryTest {
    private WeeklyReportMapper mapper;
    private MybatisWeeklyReportRepository repository;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "weekly-report-test"), WeeklyReportRow.class);
        mapper = mock(WeeklyReportMapper.class);
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

    @Test
    void listAllReportsKeepsTheirContent() {
        WeeklyReportRow row = WeeklyReportRow.fromDomain(new WeeklyReport(
                9L, 42L, LocalDate.of(2026, 8, 24), "完成登录", "联调问题", "下周验证",
                "周报.docx", Instant.parse("2026-08-24T08:00:00Z"), Instant.parse("2026-08-24T09:00:00Z")));
        when(mapper.selectList(any())).thenReturn(List.of(row));

        List<WeeklyReport> reports = repository.findAllByUserId(42L);

        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).coreWork()).isEqualTo("完成登录");
        assertThat(reports.get(0).problems()).isEqualTo("联调问题");
        assertThat(reports.get(0).nextWeekPlan()).isEqualTo("下周验证");
        assertThat(reports.get(0).sourceFileName()).isEqualTo("周报.docx");
    }

    @Test
    void searchLimitsResultsToOwnerDateKeywordAndTenRows() {
        when(mapper.selectCount(any())).thenReturn(1L);
        when(mapper.selectList(any())).thenReturn(List.of());

        var result = repository.search(42L, LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31), "登录", 2);

        ArgumentCaptor<Wrapper<WeeklyReportRow>> query = wrapperCaptor();
        verify(mapper).selectList(query.capture());
        assertThat(query.getValue().getSqlSegment())
                .contains("user_id", "week_start_date", "core_work", "problems", "next_week_plan",
                        "LIMIT 10 OFFSET 10");
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.page()).isEqualTo(2);
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
