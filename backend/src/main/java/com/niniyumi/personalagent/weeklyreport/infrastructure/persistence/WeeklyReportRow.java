package com.niniyumi.personalagent.weeklyreport.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import java.time.Instant;
import java.time.LocalDate;

@TableName("weekly_reports")
public class WeeklyReportRow {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private LocalDate weekStartDate;
    private String coreWork;
    private String problems;
    private String nextWeekPlan;
    private String sourceFileName;
    private Instant createdAt;
    private Instant updatedAt;

    public static WeeklyReportRow fromDomain(WeeklyReport report) {
        WeeklyReportRow row = new WeeklyReportRow();
        row.id = report.id();
        row.userId = report.userId();
        row.weekStartDate = report.weekStartDate();
        row.coreWork = report.coreWork();
        row.problems = report.problems();
        row.nextWeekPlan = report.nextWeekPlan();
        row.sourceFileName = report.sourceFileName();
        row.createdAt = report.createdAt();
        row.updatedAt = report.updatedAt();
        return row;
    }

    public WeeklyReport toDomain() {
        return new WeeklyReport(id, userId, weekStartDate, coreWork, problems, nextWeekPlan,
                sourceFileName, createdAt, updatedAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }
    public String getCoreWork() { return coreWork; }
    public void setCoreWork(String coreWork) { this.coreWork = coreWork; }
    public String getProblems() { return problems; }
    public void setProblems(String problems) { this.problems = problems; }
    public String getNextWeekPlan() { return nextWeekPlan; }
    public void setNextWeekPlan(String nextWeekPlan) { this.nextWeekPlan = nextWeekPlan; }
    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
