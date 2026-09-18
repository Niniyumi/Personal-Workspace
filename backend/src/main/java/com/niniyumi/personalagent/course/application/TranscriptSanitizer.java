package com.niniyumi.personalagent.course.application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class TranscriptSanitizer {
    private static final Pattern ABNORMAL_LATIN_REPETITION = Pattern.compile(
            "(?i)(?<![A-Za-z0-9])([A-Za-z][A-Za-z0-9]{1,23}?)(?:\\s*\\1){3,}(?![A-Za-z0-9])");

    public String clean(String transcript) {
        if (transcript == null || transcript.isBlank()) return transcript;
        Matcher matcher = ABNORMAL_LATIN_REPETITION.matcher(transcript);
        return matcher.replaceAll("$1");
    }
}
