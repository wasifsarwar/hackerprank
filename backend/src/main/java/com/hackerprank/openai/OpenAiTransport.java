package com.hackerprank.openai;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

public interface OpenAiTransport {
    OpenAiHttpResponse post(URI uri, String apiKey, String requestBody, Duration timeout)
        throws IOException, InterruptedException;
}
