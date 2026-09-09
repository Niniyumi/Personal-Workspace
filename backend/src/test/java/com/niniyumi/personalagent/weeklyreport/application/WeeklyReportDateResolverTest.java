package com.niniyumi.personalagent.weeklyreport.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class WeeklyReportDateResolverTest {
    private final WeeklyReportDateResolver resolver = new WeeklyReportDateResolver();

    @Test
    void usesTheEndOfAnExplicitReportPeriodAndNormalizesItToMonday() {
        assertThat(resolver.resolveWeekStart(
                "工作周报.docx", "统计周期：2026-09-04 至 2026-09-10\n完成登录模块"))
                .contains(LocalDate.of(2026, 9, 7));
    }

    @Test
    void recognizesAnyDatedFileInsteadOfRequiringThursday() {
        assertThat(resolver.resolveWeekStart("周报-2026年9月13日.docx", "本周工作"))
                .contains(LocalDate.of(2026, 9, 7));
    }

    @Test
    void supportsACompactRangeThatSharesTheStartingYear() {
        assertThat(resolver.resolveWeekStart("周报.docx", "汇报周期 2026.09.04-09.10"))
                .contains(LocalDate.of(2026, 9, 7));
    }

    @Test
    void leavesTheDateEmptyWhenTheDocumentHasNoReliableDate() {
        assertThat(resolver.resolveWeekStart("工作周报.docx", "完成了登录模块"))
                .isEmpty();
    }
}
