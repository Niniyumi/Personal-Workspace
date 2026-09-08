package com.niniyumi.personalagent.weeklyreport.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.AiProviderException;
import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.ChatProvider;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class WeeklyReportAiServiceTest {

    @Test
    void classifiesDocumentIntoTheThreeReportFields() {
        ChatProvider provider = (system, user) -> """
                {"coreWork":"完成登录","problems":"接口超时","nextWeekPlan":"开发周报"}
                """;
        WeeklyReportAiService service = new WeeklyReportAiService(provider, new ObjectMapper());

        DocumentClassification result = service.classifyDocument("原始周报文字");

        assertThat(result.coreWork()).isEqualTo("完成登录");
        assertThat(result.problems()).isEqualTo("接口超时");
        assertThat(result.nextWeekPlan()).isEqualTo("开发周报");
    }

    @Test
    void providerFailureFallsBackToAllTextInCoreWork(CapturedOutput output) {
        ChatProvider provider = (system, user) -> {
            throw new AiProviderException("provider unavailable");
        };
        WeeklyReportAiService service = new WeeklyReportAiService(provider, new ObjectMapper());

        DocumentClassification result = service.classifyDocument("原始周报文字");

        assertThat(result).isEqualTo(new DocumentClassification("原始周报文字", null, null));
        assertThat(output).contains("Weekly report document classification failed");
    }

    @Test
    void invalidOrBlankJsonFallsBackWithoutASecondModelCall() {
        ChatProvider provider = (system, user) -> "{\"coreWork\":\"   \"}";
        WeeklyReportAiService service = new WeeklyReportAiService(provider, new ObjectMapper());

        DocumentClassification result = service.classifyDocument("原始周报文字");

        assertThat(result).isEqualTo(new DocumentClassification("原始周报文字", null, null));
    }

    @Test
    void generatesAValidatedWorkSummary() {
        ChatProvider provider = (system, user) -> """
                {"coreContent":"关键项目交付","routineWork":"日常维护","selfScore":88}
                """;
        WeeklyReportAiService service = new WeeklyReportAiService(provider, new ObjectMapper());

        GeneratedWorkSummary result = service.generateSummary(List.of(report()));

        assertThat(result).isEqualTo(new GeneratedWorkSummary("关键项目交付", "日常维护", 88));
    }

    @Test
    void providerFailureIsPreservedAsTheSummaryFailureCause() {
        AiProviderException failure = new AiProviderException("provider unavailable");
        ChatProvider provider = (system, user) -> {
            throw failure;
        };
        WeeklyReportAiService service = new WeeklyReportAiService(provider, new ObjectMapper());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.generateSummary(List.of(report())))
                .isInstanceOf(SummaryGenerationException.class)
                .hasCause(failure);
    }

    private WeeklyReport report() {
        Instant now = Instant.parse("2026-08-24T10:00:00Z");
        return new WeeklyReport(9L, 42L, LocalDate.of(2026, 8, 24), "完成登录", "接口超时",
                "开发周报", null, now, now);
    }
}
