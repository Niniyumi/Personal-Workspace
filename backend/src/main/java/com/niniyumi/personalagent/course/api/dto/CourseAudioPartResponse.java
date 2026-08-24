package com.niniyumi.personalagent.course.api.dto;

import com.niniyumi.personalagent.course.domain.CourseAudioPart;

public record CourseAudioPartResponse(long id, int partNumber, int durationSeconds, long fileSize) {
    public static CourseAudioPartResponse from(CourseAudioPart part) {
        return new CourseAudioPartResponse(part.id(), part.partNumber(), part.durationSeconds(), part.fileSize());
    }
}
