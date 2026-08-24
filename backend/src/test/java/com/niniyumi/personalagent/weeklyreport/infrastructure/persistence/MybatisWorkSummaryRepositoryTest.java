package com.niniyumi.personalagent.weeklyreport.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.niniyumi.personalagent.weeklyreport.domain.SummaryPeriodType;
import com.niniyumi.personalagent.weeklyreport.domain.WorkSummary;
import java.time.Instant;
import java.time.LocalDate;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MybatisWorkSummaryRepositoryTest {
    @Mock private WorkSummaryMapper mapper;
    private MybatisWorkSummaryRepository repository;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), WorkSummaryRow.class);
        repository = new MybatisWorkSummaryRepository(mapper);
    }

    @Test
    void periodLookupAlwaysIncludesOwnerTypeAndDates() {
        repository.findByUserIdAndPeriod(
                42L, SummaryPeriodType.QUARTER,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));

        assertThat(capturedSelectOne().getSqlSegment())
                .contains("user_id", "period_type", "period_start", "period_end");
    }

    @Test
    void idLookupAndUpdateAlwaysIncludeTheOwner() {
        repository.findByIdAndUserId(7L, 42L);
        assertThat(capturedSelectOne().getSqlSegment()).contains("id", "user_id");

        repository.update(summary());
        ArgumentCaptor<Wrapper<WorkSummaryRow>> update = wrapperCaptor();
        verify(mapper).update(any(WorkSummaryRow.class), update.capture());
        assertThat(update.getValue().getSqlSegment()).contains("id", "user_id");
    }

    private WorkSummary summary() {
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        return new WorkSummary(7L, 42L, SummaryPeriodType.QUARTER,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30),
                "核心", "日常", 88, now, now, now);
    }

    private Wrapper<WorkSummaryRow> capturedSelectOne() {
        ArgumentCaptor<Wrapper<WorkSummaryRow>> query = wrapperCaptor();
        verify(mapper).selectOne(query.capture());
        return query.getValue();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentCaptor<Wrapper<WorkSummaryRow>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }
}
