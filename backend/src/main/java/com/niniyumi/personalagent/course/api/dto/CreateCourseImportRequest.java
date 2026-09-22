package com.niniyumi.personalagent.course.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCourseImportRequest(
        @NotBlank @Size(max = 160) String title,
        @Positive long fileSize,
        @NotBlank String fileName,
        java.time.LocalDate lessonDate) { }
