package com.niniyumi.personalagent.weeklyreport.application;

import java.time.LocalDate;

public record SaveWeeklyReportCommand(
        LocalDate weekStartDate,
        String coreWork,
        String problems,
        String nextWeekPlan,
        String sourceFileName) {
}
