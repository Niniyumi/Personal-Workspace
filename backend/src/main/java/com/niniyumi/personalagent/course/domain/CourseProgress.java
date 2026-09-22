package com.niniyumi.personalagent.course.domain;

public record CourseProgress(CourseStatus status, int processingProgress, String errorMessage) { }
