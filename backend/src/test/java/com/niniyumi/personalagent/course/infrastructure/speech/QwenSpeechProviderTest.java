package com.niniyumi.personalagent.course.infrastructure.speech;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QwenSpeechProviderTest {
    @TempDir
    Path tempDir;

    @Test
    void sendsWebmAudioToQwenChatCompletions() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"choices":[{"message":{"role":"assistant","content":"欢迎学习 Java。"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            byte[] audio = "fake-webm-audio".getBytes(StandardCharsets.UTF_8);
            Path audioFile = Files.write(tempDir.resolve("part-1.webm"), audio);
            SpeechProviderProperties properties = new SpeechProviderProperties(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/",
                    "test-key", "qwen3-asr-flash", 2);
            QwenSpeechProvider provider = new QwenSpeechProvider(properties);

            String transcript = provider.transcribe(audioFile);

            JsonNode body = new ObjectMapper().readTree(requestBody.get());
            String dataUrl = body.path("messages").get(0).path("content").get(0)
                    .path("input_audio").path("data").asText();
            assertThat(transcript).isEqualTo("欢迎学习 Java。");
            assertThat(authorization.get()).isEqualTo("Bearer test-key");
            assertThat(body.path("model").asText()).isEqualTo("qwen3-asr-flash");
            assertThat(body.path("stream").asBoolean()).isFalse();
            assertThat(body.path("asr_options").path("enable_itn").asBoolean()).isTrue();
            assertThat(dataUrl).startsWith("data:audio/webm;base64,");
            assertThat(Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1)))
                    .isEqualTo(audio);
        } finally {
            server.stop(0);
        }
    }
}
