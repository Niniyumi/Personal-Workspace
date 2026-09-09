package com.niniyumi.personalagent.dashboard.application;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReportRepository;
import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final WeeklyReportRepository reports;
    private final CourseRepository courses;
    private final Clock clock;

    public DashboardService(WeeklyReportRepository reports, CourseRepository courses, Clock clock) {
        this.reports = reports;
        this.courses = courses;
        this.clock = clock;
    }

    public DashboardSnapshot get(long userId) {
        List<WeeklyReport> userReports = reports.findAllByUserId(userId);
        List<Course> userCourses = courses.findAllByUserId(userId);
        ZoneId zone = clock.getZone();
        YearMonth currentMonth = YearMonth.now(clock);
        List<Course> notes = userCourses.stream().filter(this::hasNote).toList();

        List<DashboardSnapshot.MonthlyActivity> activity = new ArrayList<>();
        for (int offset = 5; offset >= 0; offset--) {
            YearMonth month = currentMonth.minusMonths(offset);
            int reportCount = (int) userReports.stream()
                    .filter(report -> YearMonth.from(report.weekStartDate()).equals(month)).count();
            int noteCount = (int) notes.stream()
                    .filter(course -> YearMonth.from(course.updatedAt().atZone(zone)).equals(month)).count();
            activity.add(new DashboardSnapshot.MonthlyActivity(month.toString(), reportCount, noteCount));
        }

        List<DashboardSnapshot.RecentItem> recent = new ArrayList<>();
        userReports.forEach(report -> recent.add(new DashboardSnapshot.RecentItem(
                "WEEKLY_REPORT", report.id(), report.weekStartDate() + " 周报", report.updatedAt())));
        userCourses.forEach(course -> recent.add(new DashboardSnapshot.RecentItem(
                "COURSE", course.id(), course.title(), course.updatedAt())));
        recent.sort(Comparator.comparing(DashboardSnapshot.RecentItem::updatedAt).reversed());

        int monthReports = (int) userReports.stream()
                .filter(report -> YearMonth.from(report.weekStartDate()).equals(currentMonth)).count();
        int monthNotes = (int) notes.stream()
                .filter(course -> YearMonth.from(course.updatedAt().atZone(zone)).equals(currentMonth)).count();
        int monthSeconds = userCourses.stream()
                .filter(course -> YearMonth.from(course.createdAt().atZone(zone)).equals(currentMonth))
                .mapToInt(Course::durationSeconds).sum();

        return new DashboardSnapshot(
                userReports.size(), notes.size(), userCourses.stream().mapToInt(Course::durationSeconds).sum(),
                monthReports, monthNotes, monthSeconds, List.copyOf(activity),
                List.copyOf(recent.subList(0, Math.min(5, recent.size()))));
    }

    private boolean hasNote(Course course) {
        return course.noteContent() != null && !course.noteContent().isBlank();
    }
}
