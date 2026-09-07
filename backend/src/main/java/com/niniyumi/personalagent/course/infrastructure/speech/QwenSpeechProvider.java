package com.niniyumi.personalagent.course.infrastructure.speech;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class QwenSpeechProvider implements SpeechProvider {
    private final SpeechProviderProperties properties;
    private final RestClient restClient;

    public QwenSpeechProvider(SpeechProviderProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(properties.timeoutSeconds());
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);
        String baseUrl = properties.baseUrl().endsWith("/")
                ? properties.baseUrl().substring(0, properties.baseUrl().length() - 1)
                : properties.baseUrl();
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public String transcribe(Path audioFile) {
        if (blank(properties.apiKey()) || blank(properties.model())) {
            throw new SpeechProviderException("Speech provider is not configured");
        }
        try {
            String dataUrl = "data:audio/webm;base64,"
                    + Base64.getEncoder().encodeToString(Files.readAllBytes(audioFile));
            QwenRequest request = new QwenRequest(
                    properties.model(),
                    List.of(new Message("user", List.of(
                            new AudioContent("input_audio", new InputAudio(dataUrl))))),
                    false,
                    new AsrOptions(true));
            QwenResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(QwenResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()
                    || response.choices().get(0).message() == null
                    || blank(response.choices().get(0).message().content())) {
                throw new SpeechProviderException("Speech provider returned no text");
            }
            return response.choices().get(0).message().content().trim();
        } catch (IOException | RestClientException exception) {
            throw new SpeechProviderException("Speech provider request failed", exception);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record QwenRequest(
            String model,
            List<Message> messages,
            boolean stream,
            AsrOptions asr_options) {
    }

    private record Message(String role, List<AudioContent> content) {
    }

    private record AudioContent(String type, InputAudio input_audio) {
    }

    private record InputAudio(String data) {
    }

    private record AsrOptions(boolean enable_itn) {
    }

    private record QwenResponse(List<Choice> choices) {
    }

    private record Choice(ResponseMessage message) {
    }

    private record ResponseMessage(String role, String content) {
    }
}
