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
        List<RecentItem> recentItems) {

    public record MonthlyActivity(String month, int weeklyReports, int courseNotes) { }

    public record RecentItem(String type, long id, String title, Instant updatedAt) { }
}
