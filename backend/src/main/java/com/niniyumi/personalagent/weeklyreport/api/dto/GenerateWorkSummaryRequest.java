package com.niniyumi.personalagent.weeklyreport.api.dto;

import com.niniyumi.personalagent.weeklyreport.domain.SummaryPeriodType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerateWorkSummaryRequest(
        @NotNull SummaryPeriodType periodType,
        @Min(2000) @Max(2100) int year,
        @Min(1) @Max(4) Integer quarter) {

    @AssertTrue
    public boolean isPeriodSelectionValid() {
        if (periodType == null) return true;
        return periodType == SummaryPeriodType.QUARTER ? quarter != null : quarter == null;
    }
}
