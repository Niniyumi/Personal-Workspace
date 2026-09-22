package com.niniyumi.personalagent.weeklyreport.api.dto;

import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReportPage;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WeeklyReportSearchResponse(List<Item> items, long total, int page) {
    public static WeeklyReportSearchResponse from(WeeklyReportPage result) {
        return new WeeklyReportSearchResponse(result.items().stream().map(Item::from).toList(),
                result.total(), result.page());
    }

    public record Item(long id, LocalDate weekStartDate, Instant createdAt,
            String sourceFileName, int characterCount, String preview) {
        static Item from(WeeklyReport report) {
            String content = report.coreWork() + " " + text(report.problems()) + " " + text(report.nextWeekPlan());
            String compact = content.replaceAll("\\s+", "");
            String preview = report.coreWork().replaceAll("\\s+", " ").trim();
            if (preview.length() > 80) preview = preview.substring(0, 80) + "…";
            return new Item(report.id(), report.weekStartDate(), report.createdAt(),
                    report.sourceFileName(), compact.codePointCount(0, compact.length()), preview);
        }

        private static String text(String value) { return value == null ? "" : value; }
    }
}
