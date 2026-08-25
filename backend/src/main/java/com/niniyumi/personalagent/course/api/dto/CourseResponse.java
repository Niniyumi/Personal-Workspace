package com.niniyumi.personalagent.course.api.dto;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import java.time.Instant;

public record CourseResponse(
        long id,
        String title,
        CourseStatus status,
        int durationSeconds,
        int processingProgress,
        String transcript,
        String noteContent,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.id(), course.title(), course.status(), course.durationSeconds(),
                course.processingProgress(), course.transcript(), course.noteContent(), course.errorMessage(),
                course.createdAt(), course.updatedAt());
    }
}
