package com.hackerprank.problems;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

interface OpenAiTransport {
    OpenAiHttpResponse post(URI uri, String apiKey, String requestBody, Duration timeout)
        throws IOException, InterruptedException;
}
