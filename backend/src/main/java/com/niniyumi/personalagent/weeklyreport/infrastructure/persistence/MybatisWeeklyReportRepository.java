package com.niniyumi.personalagent.weeklyreport.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReportRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MybatisWeeklyReportRepository implements WeeklyReportRepository {
    private final WeeklyReportMapper mapper;

    public MybatisWeeklyReportRepository(WeeklyReportMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public WeeklyReport save(WeeklyReport report) {
        WeeklyReportRow row = WeeklyReportRow.fromDomain(report);
        mapper.insert(row);
        return row.toDomain();
    }

    @Override
    public WeeklyReport update(WeeklyReport report) {
        WeeklyReportRow row = WeeklyReportRow.fromDomain(report);
        // 更新条件同时包含记录与用户，防止跨用户修改周报。
        mapper.update(row, new LambdaUpdateWrapper<WeeklyReportRow>()
                .eq(WeeklyReportRow::getId, report.id())
                .eq(WeeklyReportRow::getUserId, report.userId()));
        return row.toDomain();
    }

    @Override
    public Optional<WeeklyReport> findByIdAndUserId(long id, long userId) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<WeeklyReportRow>()
                        .eq(WeeklyReportRow::getId, id)
                        .eq(WeeklyReportRow::getUserId, userId)))
                .map(WeeklyReportRow::toDomain);
    }

    @Override
    public Optional<WeeklyReport> findByUserIdAndWeekStartDate(long userId, LocalDate weekStartDate) {
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<WeeklyReportRow>()
                        .eq(WeeklyReportRow::getUserId, userId)
                        .eq(WeeklyReportRow::getWeekStartDate, weekStartDate)))
                .map(WeeklyReportRow::toDomain);
    }

    @Override
    public List<WeeklyReport> findByUserIdAndWeekStartDateBetween(
            long userId, LocalDate startDate, LocalDate endDate) {
        return mapper.selectList(new LambdaQueryWrapper<WeeklyReportRow>()
                        .eq(WeeklyReportRow::getUserId, userId)
                        .between(WeeklyReportRow::getWeekStartDate, startDate, endDate)
                        .orderByDesc(WeeklyReportRow::getUpdatedAt))
                .stream()
                .map(WeeklyReportRow::toDomain)
                .toList();
    }

    @Override
    public List<WeeklyReport> findAllByUserId(long userId) {
        return mapper.selectList(new LambdaQueryWrapper<WeeklyReportRow>()
                        .eq(WeeklyReportRow::getUserId, userId)
                        .orderByDesc(WeeklyReportRow::getUpdatedAt))
                .stream()
                .map(WeeklyReportRow::toDomain)
                .toList();
    }
}
