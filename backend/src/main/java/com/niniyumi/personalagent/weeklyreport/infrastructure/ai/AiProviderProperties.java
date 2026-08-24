package com.niniyumi.personalagent.weeklyreport.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.ai")
public record AiProviderProperties(
        String baseUrl,
        String apiKey,
        String model,
        int timeoutSeconds) {
}
