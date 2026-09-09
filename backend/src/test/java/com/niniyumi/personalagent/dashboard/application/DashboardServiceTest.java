package com.niniyumi.personalagent.dashboard.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReportRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-08T04:00:00Z");

    @Test
    void aggregatesOwnedReportsAndCompletedNotesIntoSixMonths() {
        WeeklyReport august = report(1L, LocalDate.of(2026, 8, 3), "2026-08-04T04:00:00Z");
        WeeklyReport september = report(2L, LocalDate.of(2026, 9, 7), "2026-09-08T03:00:00Z");
        Course ready = course(3L, CourseStatus.READY, 600, "笔记", "2026-09-08T02:00:00Z");
        Course transcribed = course(4L, CourseStatus.TRANSCRIBED, 300, null, "2026-09-07T02:00:00Z");
        DashboardService service = new DashboardService(
                new Reports(List.of(august, september)),
                new Courses(List.of(ready, transcribed)),
                Clock.fixed(NOW, ZoneId.of("Asia/Shanghai")));

        DashboardSnapshot result = service.get(42L);

        assertThat(result.totalWeeklyReports()).isEqualTo(2);
        assertThat(result.totalCourseNotes()).isEqualTo(1);
        assertThat(result.totalRecordingSeconds()).isEqualTo(900);
        assertThat(result.monthWeeklyReports()).isEqualTo(1);
        assertThat(result.monthCourseNotes()).isEqualTo(1);
        assertThat(result.monthRecordingSeconds()).isEqualTo(900);
        assertThat(result.monthlyActivity()).hasSize(6);
        assertThat(result.monthlyActivity().get(4).month()).isEqualTo("2026-08");
        assertThat(result.monthlyActivity().get(4).weeklyReports()).isEqualTo(1);
        assertThat(result.monthlyActivity().get(5).courseNotes()).isEqualTo(1);
        assertThat(result.recentItems()).extracting(DashboardSnapshot.RecentItem::id)
                .containsExactly(2L, 3L, 4L, 1L);
    }

    private WeeklyReport report(long id, LocalDate week, String updated) {
        Instant time = Instant.parse(updated);
        return new WeeklyReport(id, 42L, week, "工作", null, null, null, time, time);
    }

    private Course course(long id, CourseStatus status, int seconds, String note, String updated) {
        Instant time = Instant.parse(updated);
        return new Course(id, 42L, "课程 " + id, status, seconds, 100,
                "转写", note, null, time, time);
    }

    private record Reports(List<WeeklyReport> values) implements WeeklyReportRepository {
        public WeeklyReport save(WeeklyReport value) { return value; }
        public WeeklyReport update(WeeklyReport value) { return value; }
        public Optional<WeeklyReport> findByIdAndUserId(long id, long userId) { return Optional.empty(); }
        public Optional<WeeklyReport> findByUserIdAndWeekStartDate(long userId, LocalDate date) { return Optional.empty(); }
        public List<WeeklyReport> findByUserIdAndWeekStartDateBetween(long userId, LocalDate start, LocalDate end) { return values; }
        public List<WeeklyReport> findAllByUserId(long userId) { return values; }
    }

    private record Courses(List<Course> values) implements CourseRepository {
        public Course save(Course value) { return value; }
        public Course update(Course value) { return value; }
        public Optional<Course> findByIdAndUserId(long id, long userId) { return Optional.empty(); }
        public List<Course> findAllByUserId(long userId) { return values; }
    }
}
