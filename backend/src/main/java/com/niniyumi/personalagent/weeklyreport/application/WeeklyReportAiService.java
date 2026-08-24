package com.niniyumi.personalagent.weeklyreport.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.AiProviderException;
import com.niniyumi.personalagent.weeklyreport.infrastructure.ai.ChatProvider;
import org.springframework.stereotype.Service;

@Service
public class WeeklyReportAiService {
    private static final String SYSTEM_PROMPT = """
            你是周报整理助手。请把用户提供的文字归类为 JSON 对象，只包含以下三个字段：
            coreWork、problems、nextWeekPlan。字段值必须是字符串或 null。
            无法确定分类的内容全部放入 coreWork，coreWork 不得为空。不要输出 Markdown。
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
            return new DocumentClassification(extractedText, null, null);
        }
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
}
