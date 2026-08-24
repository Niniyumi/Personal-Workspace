package com.niniyumi.personalagent.weeklyreport.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.AiProviderException;
import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.ChatProvider;
import org.junit.jupiter.api.Test;

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
    void providerFailureFallsBackToAllTextInCoreWork() {
        ChatProvider provider = (system, user) -> {
            throw new AiProviderException("provider unavailable");
        };
        WeeklyReportAiService service = new WeeklyReportAiService(provider, new ObjectMapper());

        DocumentClassification result = service.classifyDocument("原始周报文字");

        assertThat(result).isEqualTo(new DocumentClassification("原始周报文字", null, null));
    }

    @Test
    void invalidOrBlankJsonFallsBackWithoutASecondModelCall() {
        ChatProvider provider = (system, user) -> "{\"coreWork\":\"   \"}";
        WeeklyReportAiService service = new WeeklyReportAiService(provider, new ObjectMapper());

        DocumentClassification result = service.classifyDocument("原始周报文字");

        assertThat(result).isEqualTo(new DocumentClassification("原始周报文字", null, null));
    }
}
