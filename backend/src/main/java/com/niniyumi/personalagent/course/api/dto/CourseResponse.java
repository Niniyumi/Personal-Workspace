package com.niniyumi.personalagent.course.api.dto;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import java.time.Instant;

public record CourseResponse(
        long id,
        String title,
        String courseName,
        java.time.LocalDate lessonDate,
        CourseStatus status,
        int durationSeconds,
        int processingProgress,
        String transcript,
        String noteContent,
        String noteCandidate,
        String errorMessage,
        String sourceType,
        Instant createdAt,
        Instant updatedAt) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.id(), course.title(), course.courseName(), course.lessonDate(), course.status(), course.durationSeconds(),
                course.processingProgress(), course.transcript(), course.noteContent(), course.noteCandidate(), course.errorMessage(),
                course.sourceType(),
                course.createdAt(), course.updatedAt());
    }
}
