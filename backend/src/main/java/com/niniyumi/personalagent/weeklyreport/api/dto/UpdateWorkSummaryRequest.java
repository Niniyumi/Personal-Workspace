package com.niniyumi.personalagent.weeklyreport.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateWorkSummaryRequest(
        @NotBlank String coreContent,
        @NotBlank String routineWork,
        @Min(0) @Max(100) int selfScore) {
}
