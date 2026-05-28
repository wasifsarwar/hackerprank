package com.hackerprank.problems;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.stereotype.Component;

@Component
class JdkOpenAiTransport implements OpenAiTransport {
    private final HttpClient httpClient;

    JdkOpenAiTransport() {
        this(HttpClient.newHttpClient());
    }

    JdkOpenAiTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public OpenAiHttpResponse post(URI uri, String apiKey, String requestBody, Duration timeout)
        throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(timeout)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(
            request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        return new OpenAiHttpResponse(response.statusCode(), response.body());
    }
}
