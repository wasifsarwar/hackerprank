package com.hackerprank.problems;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hackerprank.openai")
class OpenAiProblemGeneratorProperties {
    private String apiKey = "";
    private String model = "gpt-5-mini";
    private String responsesUrl = "https://api.openai.com/v1/responses";
    private int timeoutSeconds = 45;
    private int maxOutputTokens = 6000;
    private String promptVersion = "openai-problem-v1";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getResponsesUrl() {
        return responsesUrl;
    }

    public void setResponsesUrl(String responsesUrl) {
        this.responsesUrl = responsesUrl;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    public void setMaxOutputTokens(int maxOutputTokens) {
        this.maxOutputTokens = maxOutputTokens;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    URI responsesUri() {
        return URI.create(responsesUrl);
    }
}
