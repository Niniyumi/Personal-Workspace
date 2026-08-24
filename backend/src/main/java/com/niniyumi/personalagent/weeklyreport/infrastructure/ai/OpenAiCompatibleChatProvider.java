package com.niniyumi.personalagent.weeklyreport.infrastructure.ai;

import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiCompatibleChatProvider implements ChatProvider {
    private final AiProviderProperties properties;
    private final RestClient restClient;

    public OpenAiCompatibleChatProvider(AiProviderProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.timeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        String baseUrl = properties.baseUrl().endsWith("/")
                ? properties.baseUrl().substring(0, properties.baseUrl().length() - 1)
                : properties.baseUrl();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (isBlank(properties.apiKey()) || isBlank(properties.model())) {
            throw new AiProviderException("AI provider is not configured");
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                properties.model(),
                List.of(new ChatMessage("system", systemPrompt), new ChatMessage("user", userPrompt)),
                0);
        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().getFirst().message() == null
                    || isBlank(response.choices().getFirst().message().content())) {
                throw new AiProviderException("AI provider returned no content");
            }
            return response.choices().getFirst().message().content();
        } catch (RestClientException exception) {
            throw new AiProviderException("AI provider request failed", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            int temperature) {
    }

    private record ChatCompletionResponse(List<Choice> choices) {
    }

    private record Choice(ChatMessage message) {
    }

    private record ChatMessage(String role, String content) {
    }
}
