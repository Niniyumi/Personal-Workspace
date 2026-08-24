package com.niniyumi.personalagent.course.infrastructure.speech;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.speech")
public record SpeechProviderProperties(
        String baseUrl,
        String apiKey,
        String model,
        int timeoutSeconds) {
}
