package com.niniyumi.personalagent.course.domain;

import java.time.Instant;

public record CourseAudioPart(
        Long id,
        long courseId,
        int partNumber,
        int durationSeconds,
        String storagePath,
        long fileSize,
        Instant createdAt) {
}
