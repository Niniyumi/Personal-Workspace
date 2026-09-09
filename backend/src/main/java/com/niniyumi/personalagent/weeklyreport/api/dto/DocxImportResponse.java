package com.niniyumi.personalagent.weeklyreport.api.dto;

import com.niniyumi.personalagent.weeklyreport.application.DocumentClassification;
import java.time.LocalDate;

public record DocxImportResponse(
        LocalDate weekStartDate,
        String coreWork,
        String problems,
        String nextWeekPlan,
        String sourceFileName) {

    public static DocxImportResponse from(
            DocumentClassification classification, String sourceFileName, LocalDate weekStartDate) {
        return new DocxImportResponse(
                weekStartDate, classification.coreWork(), classification.problems(),
                classification.nextWeekPlan(), sourceFileName);
    }
}
