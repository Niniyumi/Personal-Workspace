package com.niniyumi.personalagent.course.domain;

import java.time.Instant;

public record Course(
        Long id,
        long userId,
        String title,
        CourseStatus status,
        int durationSeconds,
        String transcript,
        String noteContent,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt) {
}
