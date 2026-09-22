package com.niniyumi.personalagent.dashboard.application;

import java.time.Instant;
import java.util.List;

public record DashboardSnapshot(
        int totalWeeklyReports,
        int totalCourseNotes,
        int totalRecordingSeconds,
        int monthWeeklyReports,
        int monthCourseNotes,
        int monthRecordingSeconds,
        List<MonthlyActivity> monthlyActivity,
        List<YearlyWeeklyReports> yearlyWeeklyReports,
        List<RecentItem> recentItems) {

    public record MonthlyActivity(String month, int weeklyReports, int courseNotes) { }

    public record YearlyWeeklyReports(int year, int count) { }

    public record RecentItem(String type, long id, String title, Instant updatedAt) { }
}
