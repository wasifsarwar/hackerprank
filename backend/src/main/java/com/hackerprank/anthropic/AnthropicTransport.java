package com.hackerprank.anthropic;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

public interface AnthropicTransport {
    AnthropicHttpResponse post(
        URI uri,
        String apiKey,
        String anthropicVersion,
        String requestBody,
        Duration timeout
    ) throws IOException, InterruptedException;
}
