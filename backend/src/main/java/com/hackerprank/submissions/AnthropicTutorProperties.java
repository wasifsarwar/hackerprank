package com.hackerprank.submissions;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hackerprank.tutor.anthropic")
class AnthropicTutorProperties {
    private String apiKey = "";
    private String model = "claude-sonnet-4-20250514";
    private String messagesUrl = "https://api.anthropic.com/v1/messages";
    private String version = "2023-06-01";
    private int timeoutSeconds = 30;
    private int maxOutputTokens = 900;
    private String promptVersion = "anthropic-tutor-v1";

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

    public String getMessagesUrl() {
        return messagesUrl;
    }

    public void setMessagesUrl(String messagesUrl) {
        this.messagesUrl = messagesUrl;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    URI messagesUri() {
        return URI.create(messagesUrl);
    }
}
