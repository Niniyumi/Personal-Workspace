package com.niniyumi.personalagent.course.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TranscriptSanitizerTest {
    private final TranscriptSanitizer sanitizer = new TranscriptSanitizer();

    @Test
    void collapsesAnAbnormallyRepeatedEnglishTokenWithoutAWordBlacklist() {
        String transcript = "老师开始讲组织行为学。" + "Jerrycat".repeat(71) + "下面进入案例。";

        assertThat(sanitizer.clean(transcript))
                .isEqualTo("老师开始讲组织行为学。Jerrycat下面进入案例。");
    }

    @Test
    void keepsOrdinaryShortRepetition() {
        assertThat(sanitizer.clean("This is very very important. 好好学习。"))
                .isEqualTo("This is very very important. 好好学习。");
    }
}
