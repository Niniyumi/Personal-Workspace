package com.niniyumi.personalagent.weeklyreport.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleChatProviderTest {

    @Test
    void sendsOneOpenAiCompatibleChatCompletionRequest() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = """
                    {"choices":[{"message":{"role":"assistant","content":"{\\"coreWork\\":\\"完成登录\\"}"}}]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        try {
            AiProviderProperties properties = new AiProviderProperties(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/",
                    "test-key", "test-model", 2);
            OpenAiCompatibleChatProvider provider = new OpenAiCompatibleChatProvider(properties);

            String result = provider.complete("系统要求", "周报原文");

            JsonNode body = new ObjectMapper().readTree(requestBody.get());
            assertThat(result).isEqualTo("{\"coreWork\":\"完成登录\"}");
            assertThat(authorization.get()).isEqualTo("Bearer test-key");
            assertThat(body.path("model").asText()).isEqualTo("test-model");
            assertThat(body.path("temperature").asInt()).isZero();
            assertThat(body.path("messages")).hasSize(2);
        } finally {
            server.stop(0);
        }
    }
}
