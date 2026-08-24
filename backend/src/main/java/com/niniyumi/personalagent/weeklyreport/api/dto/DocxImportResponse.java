package com.niniyumi.personalagent.weeklyreport.api.dto;

import com.niniyumi.personalagent.weeklyreport.application.DocumentClassification;

public record DocxImportResponse(
        String coreWork,
        String problems,
        String nextWeekPlan,
        String sourceFileName) {

    public static DocxImportResponse from(DocumentClassification classification, String sourceFileName) {
        return new DocxImportResponse(
                classification.coreWork(), classification.problems(),
                classification.nextWeekPlan(), sourceFileName);
    }
}
