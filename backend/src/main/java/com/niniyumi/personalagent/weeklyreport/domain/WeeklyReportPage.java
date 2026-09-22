package com.niniyumi.personalagent.weeklyreport.domain;

import java.util.List;

public record WeeklyReportPage(List<WeeklyReport> items, long total, int page) { }
