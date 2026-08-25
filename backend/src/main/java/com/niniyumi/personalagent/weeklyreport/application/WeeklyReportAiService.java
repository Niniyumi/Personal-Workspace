package com.niniyumi.personalagent.weeklyreport.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.AiProviderException;
import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.ChatProvider;
import com.niniyumi.personalagent.weeklyreport.domain.WeeklyReport;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WeeklyReportAiService {
    private static final Logger log = LoggerFactory.getLogger(WeeklyReportAiService.class);
    private static final String SYSTEM_PROMPT = """
            你是周报整理助手。请把用户提供的文字归类为 JSON 对象，只包含以下三个字段：
            coreWork、problems、nextWeekPlan。字段值必须是字符串或 null。
            无法确定分类的内容全部放入 coreWork，coreWork 不得为空。不要输出 Markdown。
            """;
    private static final String SUMMARY_SYSTEM_PROMPT = """
            你是工作总结助手。请根据用户提供的周报生成 JSON 对象，只包含以下三个字段：
            coreContent、routineWork、selfScore。前两个字段必须是非空字符串，selfScore 必须是 0 到 100 的整数。
            核心内容突出成果和重要项目，日常工作归纳重复性维护与协作。不要输出 Markdown。
            """;

    private final ChatProvider provider;
    private final ObjectMapper objectMapper;

    public WeeklyReportAiService(ChatProvider provider, ObjectMapper objectMapper) {
        this.provider = provider;
        this.objectMapper = objectMapper;
    }

    public DocumentClassification classifyDocument(String extractedText) {
        try {
            String response = provider.complete(SYSTEM_PROMPT, extractedText);
            DocumentClassification parsed = objectMapper.readValue(response, DocumentClassification.class);
            String coreWork = required(parsed.coreWork());
            return new DocumentClassification(
                    coreWork, optional(parsed.problems()), optional(parsed.nextWeekPlan()));
        } catch (AiProviderException | JsonProcessingException | IllegalArgumentException exception) {
            // 模型不可用或格式不可靠时不重试，直接把原文放入核心工作，保证用户仍可继续编辑。
            log.warn("Weekly report document classification failed; using extracted text fallback: {}",
                    exception.getMessage());
            return new DocumentClassification(extractedText, null, null);
        }
    }

    public GeneratedWorkSummary generateSummary(List<WeeklyReport> reports) {
        try {
            String response = provider.complete(SUMMARY_SYSTEM_PROMPT, summaryInput(reports));
            SummaryPayload parsed = objectMapper.readValue(response, SummaryPayload.class);
            String coreContent = required(parsed.coreContent());
            String routineWork = required(parsed.routineWork());
            if (parsed.selfScore() == null || parsed.selfScore() < 0 || parsed.selfScore() > 100) {
                throw new IllegalArgumentException("selfScore must be between 0 and 100");
            }
            return new GeneratedWorkSummary(coreContent, routineWork, parsed.selfScore());
        } catch (AiProviderException | JsonProcessingException | IllegalArgumentException exception) {
            // 汇总失败时不覆盖旧总结，交由接口明确提示用户稍后重试。
            log.error("Work summary generation failed: {}", exception.getMessage());
            throw new SummaryGenerationException();
        }
    }

    private String summaryInput(List<WeeklyReport> reports) {
        StringBuilder input = new StringBuilder();
        for (WeeklyReport report : reports) {
            input.append("周：").append(report.weekStartDate()).append('\n')
                    .append("核心工作：").append(report.coreWork()).append('\n')
                    .append("遇到的问题：").append(report.problems() == null ? "无" : report.problems()).append('\n')
                    .append("下周计划：").append(report.nextWeekPlan() == null ? "无" : report.nextWeekPlan())
                    .append("\n\n");
        }
        return input.toString().trim();
    }

    private String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("coreWork is required");
        }
        return value.trim();
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record SummaryPayload(String coreContent, String routineWork, Integer selfScore) {
    }
}
