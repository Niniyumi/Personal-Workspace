package com.niniyumi.personalagent.course.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    Course save(Course course);

    Course update(Course course);

    default Course updateProgress(Course course) {
        return update(course);
    }

    default boolean updateTitle(long id, long userId, String title, Instant updatedAt) {
        Optional<Course> current = findByIdAndUserId(id, userId);
        if (current.isEmpty()) return false;
        Course course = current.get();
        update(new Course(course.id(), course.userId(), title, course.status(), course.durationSeconds(),
                course.processingProgress(), course.transcript(), course.noteContent(), course.errorMessage(),
                course.sourceType(), course.originalAudioPath(), course.expectedBytes(), course.noteCandidate(),
                course.createdAt(), updatedAt, course.courseName(), course.lessonDate()));
        return true;
    }

    Optional<Course> findByIdAndUserId(long id, long userId);

    default Optional<CourseProgress> findProgressByIdAndUserId(long id, long userId) {
        return findByIdAndUserId(id, userId)
                .map(course -> new CourseProgress(
                        course.status(), course.processingProgress(), course.errorMessage()));
    }

    List<Course> findAllByUserId(long userId);
}
