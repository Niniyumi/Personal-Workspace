package com.niniyumi.personalagent.course.domain;

import java.time.Instant;

public record CourseAudioPart(
        Long id,
        long courseId,
        int partNumber,
        int durationSeconds,
        String storagePath,
        long fileSize,
        String transcript,
        Instant createdAt) {
    public CourseAudioPart(Long id, long courseId, int partNumber, int durationSeconds,
            String storagePath, long fileSize, Instant createdAt) {
        this(id, courseId, partNumber, durationSeconds, storagePath, fileSize, null, createdAt);
    }
}
