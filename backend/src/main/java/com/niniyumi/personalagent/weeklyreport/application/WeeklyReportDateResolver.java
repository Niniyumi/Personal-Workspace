package com.niniyumi.personalagent.weeklyreport.application;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class WeeklyReportDateResolver {
    private static final Pattern FULL_DATE = Pattern.compile(
            "(?<!\\d)(\\d{4})\\s*[年./-]\\s*(\\d{1,2})\\s*[月./-]\\s*(\\d{1,2})\\s*日?");
    private static final Pattern COMPACT_RANGE_END = Pattern.compile(
            "(?:至|到|—|–|~|～|-)\\s*(\\d{1,2})\\s*[月./-]\\s*(\\d{1,2})\\s*日?");
    private static final Pattern PERIOD_LABEL = Pattern.compile("周期|日期|时间范围");

    public Optional<LocalDate> resolveWeekStart(String fileName, String extractedText) {
        Optional<LocalDate> reportPeriod = reportPeriodDate(extractedText);
        Optional<LocalDate> detected = reportPeriod
                .or(() -> lastDate(fileName))
                .or(() -> lastDate(extractedText == null ? null
                        : extractedText.substring(0, Math.min(800, extractedText.length()))));
        return detected.map(date -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    }

    private Optional<LocalDate> reportPeriodDate(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        return text.lines().limit(12)
                .filter(line -> PERIOD_LABEL.matcher(line).find())
                .map(this::lastDate)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<LocalDate> lastDate(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        Matcher matcher = FULL_DATE.matcher(text);
        LocalDate last = null;
        int lastEnd = -1;
        while (matcher.find()) {
            LocalDate parsed = date(matcher.group(1), matcher.group(2), matcher.group(3));
            if (parsed != null) {
                last = parsed;
                lastEnd = matcher.end();
            }
        }
        if (last == null) return Optional.empty();

        Matcher compactEnd = COMPACT_RANGE_END.matcher(text);
        compactEnd.region(lastEnd, text.length());
        if (compactEnd.find()) {
            LocalDate parsedEnd = date(Integer.toString(last.getYear()), compactEnd.group(1), compactEnd.group(2));
            if (parsedEnd != null) last = parsedEnd;
        }
        return Optional.of(last);
    }

    private LocalDate date(String year, String month, String day) {
        try {
            return LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
        } catch (DateTimeException | NumberFormatException ignored) {
            return null;
        }
    }
}
