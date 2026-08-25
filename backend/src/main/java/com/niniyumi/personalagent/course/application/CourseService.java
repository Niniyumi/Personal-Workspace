package com.niniyumi.personalagent.course.application;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseRepository;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CourseService {
    private final CourseRepository repository;
    private final Clock clock;

    public CourseService(CourseRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Course create(long userId, String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidCourseTitleException();
        }
        Instant now = clock.instant();
        return repository.save(new Course(
                null, userId, title.trim(), CourseStatus.RECORDING, 0,
                0, null, null, null, now, now));
    }

    public Course get(long userId, long courseId) {
        // 查询条件始终带 userId，避免仅凭课程编号读取其他用户的数据。
        return repository.findByIdAndUserId(courseId, userId)
                .orElseThrow(CourseNotFoundException::new);
    }

    public List<Course> list(long userId) {
        return repository.findAllByUserId(userId);
    }

    public Course saveNote(long userId, long courseId, String noteContent) {
        Course current = get(userId, courseId);
        if (current.status() != CourseStatus.READY) {
            throw new InvalidCourseStateException();
        }
        Course updated = new Course(
                current.id(), current.userId(), current.title(), current.status(),
                current.durationSeconds(), current.processingProgress(), current.transcript(), normalize(noteContent),
                current.errorMessage(), current.createdAt(), clock.instant());
        return repository.update(updated);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
