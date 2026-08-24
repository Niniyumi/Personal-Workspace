package com.niniyumi.personalagent.weeklyreport.api.dto;

import com.niniyumi.personalagent.weeklyreport.application.SaveWeeklyReportCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record WeeklyReportRequest(
        @NotNull LocalDate weekStartDate,
        @NotBlank String coreWork,
        String problems,
        String nextWeekPlan,
        @Size(max = 255) String sourceFileName) {

    public SaveWeeklyReportCommand toCommand() {
        return new SaveWeeklyReportCommand(
                weekStartDate, coreWork, problems, nextWeekPlan, sourceFileName);
    }
}
