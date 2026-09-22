package com.niniyumi.personalagent.course.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCourseTitleRequest(
        @NotBlank @Size(max = 160) String title) { }
