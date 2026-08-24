package com.niniyumi.personalagent.weeklyreport.infrastructure.ai;

@FunctionalInterface
public interface ChatProvider {
    String complete(String systemPrompt, String userPrompt);
}
