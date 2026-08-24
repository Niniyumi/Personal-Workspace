package com.niniyumi.personalagent.weeklyreport.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.niniyumi.personalagent.weeklyreport.domain.SummaryPeriodType;
import com.niniyumi.personalagent.weeklyreport.domain.WorkSummary;
import com.niniyumi.personalagent.weeklyreport.domain.WorkSummaryRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisWorkSummaryRepository implements WorkSummaryRepository {
    private final WorkSummaryMapper mapper;

    public MybatisWorkSummaryRepository(WorkSummaryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public WorkSummary save(WorkSummary summary) {
        WorkSummaryRow row = WorkSummaryRow.fromDomain(summary);
        mapper.insert(row);
        return row.toDomain();
    }

    @Override
    public WorkSummary update(WorkSummary summary) {
        WorkSummaryRow row = WorkSummaryRow.fromDomain(summary);
        // 总结的更新条件始终携带 user_id，直接访问其他用户 ID 时不会命中。
        mapper.update(row, new LambdaUpdateWrapper<WorkSummaryRow>()
                .eq(WorkSummaryRow::getId, summary.id())
                .eq(WorkSummaryRow::getUserId, summary.userId()));
        return row.toDomain();
    }

    @Override
    public Optional<WorkSummary> findByUserIdAndPeriod(
            long userId, SummaryPeriodType type, LocalDate start, LocalDate end) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<WorkSummaryRow>()
                        .eq(WorkSummaryRow::getUserId, userId)
                        .eq(WorkSummaryRow::getPeriodType, type.name())
                        .eq(WorkSummaryRow::getPeriodStart, start)
                        .eq(WorkSummaryRow::getPeriodEnd, end)))
                .map(WorkSummaryRow::toDomain);
    }

    @Override
    public Optional<WorkSummary> findByIdAndUserId(long id, long userId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<WorkSummaryRow>()
                        .eq(WorkSummaryRow::getId, id)
                        .eq(WorkSummaryRow::getUserId, userId)))
                .map(WorkSummaryRow::toDomain);
    }
}
