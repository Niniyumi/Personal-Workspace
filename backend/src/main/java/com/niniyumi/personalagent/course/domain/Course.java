package com.niniyumi.personalagent.course.domain;

import java.time.Instant;

public record Course(
        Long id,
        long userId,
        String title,
        CourseStatus status,
        int durationSeconds,
        int processingProgress,
        String transcript,
        String noteContent,
        String errorMessage,
        String sourceType,
        String originalAudioPath,
        Long expectedBytes,
        String noteCandidate,
        Instant createdAt,
        Instant updatedAt,
        String courseName,
        java.time.LocalDate lessonDate) {
    public Course withLesson(String name, java.time.LocalDate date) {
        return new Course(id, userId, title, status, durationSeconds, processingProgress, transcript,
                noteContent, errorMessage, sourceType, originalAudioPath, expectedBytes, noteCandidate,
                createdAt, updatedAt, name, date);
    }

    public Course(Long id, long userId, String title, CourseStatus status, int durationSeconds,
            int processingProgress, String transcript, String noteContent, String errorMessage,
            String sourceType, String originalAudioPath, Long expectedBytes, String noteCandidate,
            Instant createdAt, Instant updatedAt) {
        this(id, userId, title, status, durationSeconds, processingProgress, transcript, noteContent,
                errorMessage, sourceType, originalAudioPath, expectedBytes, noteCandidate, createdAt, updatedAt,
                title, createdAt.atZone(java.time.ZoneId.of("Asia/Shanghai")).toLocalDate());
    }
    public Course(Long id, long userId, String title, CourseStatus status, int durationSeconds,
            int processingProgress, String transcript, String noteContent, String errorMessage,
            String sourceType, String originalAudioPath, Long expectedBytes,
            Instant createdAt, Instant updatedAt) {
        this(id, userId, title, status, durationSeconds, processingProgress, transcript, noteContent,
                errorMessage, sourceType, originalAudioPath, expectedBytes, null, createdAt, updatedAt);
    }

    public Course(Long id, long userId, String title, CourseStatus status, int durationSeconds,
            int processingProgress, String transcript, String noteContent, String errorMessage,
            Instant createdAt, Instant updatedAt) {
        this(id, userId, title, status, durationSeconds, processingProgress, transcript, noteContent,
                errorMessage, "RECORDING", null, null, null, createdAt, updatedAt);
    }
}
