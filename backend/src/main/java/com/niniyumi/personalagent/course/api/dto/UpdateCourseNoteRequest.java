package com.niniyumi.personalagent.course.api.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCourseNoteRequest(@NotBlank String noteContent) {
}
