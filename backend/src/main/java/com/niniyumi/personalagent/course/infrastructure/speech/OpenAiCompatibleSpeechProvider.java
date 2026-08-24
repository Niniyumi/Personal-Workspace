package com.niniyumi.personalagent.course.infrastructure.speech;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OpenAiCompatibleSpeechProvider implements SpeechProvider {
    private final SpeechProviderProperties properties;
    private final RestClient restClient;

    public OpenAiCompatibleSpeechProvider(SpeechProviderProperties properties) {
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
        MultipartBodyBuilder body = new MultipartBodyBuilder();
        body.part("file", new FileSystemResource(audioFile));
        body.part("model", properties.model());
        try {
            TranscriptionResponse response = restClient.post()
                    .uri("/audio/transcriptions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body.build())
                    .retrieve()
                    .body(TranscriptionResponse.class);
            if (response == null || blank(response.text())) {
                throw new SpeechProviderException("Speech provider returned no text");
            }
            return response.text().trim();
        } catch (RestClientException exception) {
            throw new SpeechProviderException("Speech provider request failed", exception);
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record TranscriptionResponse(String text) {
    }
}
