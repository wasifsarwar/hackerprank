package com.hackerprank.anthropic;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.stereotype.Component;

@Component
public class JdkAnthropicTransport implements AnthropicTransport {
    private final HttpClient httpClient;

    public JdkAnthropicTransport() {
        this(HttpClient.newHttpClient());
    }

    JdkAnthropicTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public AnthropicHttpResponse post(
        URI uri,
        String apiKey,
        String anthropicVersion,
        String requestBody,
        Duration timeout
    ) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(timeout)
            .header("x-api-key", apiKey)
            .header("anthropic-version", anthropicVersion)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        return new AnthropicHttpResponse(response.statusCode(), response.body());
    }
}
