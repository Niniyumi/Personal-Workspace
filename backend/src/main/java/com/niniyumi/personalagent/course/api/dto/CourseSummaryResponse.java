package com.niniyumi.personalagent.course.api.dto;

import com.niniyumi.personalagent.course.domain.Course;
import com.niniyumi.personalagent.course.domain.CourseStatus;
import java.time.Instant;

public record CourseSummaryResponse(
        long id,
        String title,
        CourseStatus status,
        int durationSeconds,
        int processingProgress,
        String errorMessage,
        String sourceType,
        Instant createdAt,
        Instant updatedAt) {
    public static CourseSummaryResponse from(Course course) {
        return new CourseSummaryResponse(
                course.id(), course.title(), course.status(), course.durationSeconds(),
                course.processingProgress(), course.errorMessage(), course.sourceType(),
                course.createdAt(), course.updatedAt());
    }
}
