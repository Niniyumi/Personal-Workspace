package com.niniyumi.personalagent.weeklyreport.api;

import com.niniyumi.personalagent.auth.infrastructure.security.AuthenticatedUser;
import com.niniyumi.personalagent.weeklyreport.api.dto.GenerateWorkSummaryRequest;
import com.niniyumi.personalagent.weeklyreport.api.dto.UpdateWorkSummaryRequest;
import com.niniyumi.personalagent.weeklyreport.api.dto.WorkSummaryResponse;
import com.niniyumi.personalagent.weeklyreport.application.WorkSummaryService;
import com.niniyumi.personalagent.weeklyreport.application.InvalidSummaryPeriodException;
import com.niniyumi.personalagent.weeklyreport.domain.SummaryPeriodType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.time.Month;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/work-summaries")
public class WorkSummaryController {
    private final WorkSummaryService service;

    public WorkSummaryController(WorkSummaryService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    public WorkSummaryResponse generate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody GenerateWorkSummaryRequest request) {
        if (request.periodType() == SummaryPeriodType.YEAR) {
            return WorkSummaryResponse.from(service.generateYear(user.userId(), request.year()));
        }
        return WorkSummaryResponse.from(
                service.generateQuarter(user.userId(), request.year(), request.quarter()));
    }

    @GetMapping
    public WorkSummaryResponse get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam SummaryPeriodType periodType,
            @RequestParam @Min(2000) @Max(2100) int year,
            @RequestParam(required = false) @Min(1) @Max(4) Integer quarter) {
        PeriodRange range = periodRange(periodType, year, quarter);
        return WorkSummaryResponse.from(
                service.get(user.userId(), periodType, range.start(), range.end()));
    }

    @PutMapping("/{summaryId}")
    public WorkSummaryResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable long summaryId,
            @Valid @RequestBody UpdateWorkSummaryRequest request) {
        return WorkSummaryResponse.from(service.update(
                user.userId(), summaryId,
                request.coreContent(), request.routineWork(), request.selfScore()));
    }

    private PeriodRange periodRange(SummaryPeriodType type, int year, Integer quarter) {
        if (type == SummaryPeriodType.YEAR) {
            if (quarter != null) throw new InvalidSummaryPeriodException();
            return new PeriodRange(
                    LocalDate.of(year, Month.JANUARY, 1), LocalDate.of(year, Month.DECEMBER, 31));
        }
        if (quarter == null) throw new InvalidSummaryPeriodException();
        int startMonth = (quarter - 1) * 3 + 1;
        LocalDate start = LocalDate.of(year, startMonth, 1);
        LocalDate thirdMonth = start.plusMonths(2);
        return new PeriodRange(start, thirdMonth.withDayOfMonth(thirdMonth.lengthOfMonth()));
    }

    private record PeriodRange(LocalDate start, LocalDate end) {
    }
}
