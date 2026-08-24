package com.niniyumi.personalagent.weeklyreport.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.niniyumi.personalagent.weeklyreport.domain.SummaryPeriodType;
import com.niniyumi.personalagent.weeklyreport.domain.WorkSummary;
import java.time.Instant;
import java.time.LocalDate;

@TableName("work_summaries")
public class WorkSummaryRow {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String coreContent;
    private String routineWork;
    private Integer selfScore;
    private Instant generatedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static WorkSummaryRow fromDomain(WorkSummary summary) {
        WorkSummaryRow row = new WorkSummaryRow();
        row.id = summary.id();
        row.userId = summary.userId();
        row.periodType = summary.periodType().name();
        row.periodStart = summary.periodStart();
        row.periodEnd = summary.periodEnd();
        row.coreContent = summary.coreContent();
        row.routineWork = summary.routineWork();
        row.selfScore = summary.selfScore();
        row.generatedAt = summary.generatedAt();
        row.createdAt = summary.createdAt();
        row.updatedAt = summary.updatedAt();
        return row;
    }

    public WorkSummary toDomain() {
        return new WorkSummary(id, userId, SummaryPeriodType.valueOf(periodType), periodStart, periodEnd,
                coreContent, routineWork, selfScore, generatedAt, createdAt, updatedAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public String getCoreContent() { return coreContent; }
    public void setCoreContent(String coreContent) { this.coreContent = coreContent; }
    public String getRoutineWork() { return routineWork; }
    public void setRoutineWork(String routineWork) { this.routineWork = routineWork; }
    public Integer getSelfScore() { return selfScore; }
    public void setSelfScore(Integer selfScore) { this.selfScore = selfScore; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
