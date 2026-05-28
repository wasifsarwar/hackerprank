package com.hackerprank.problems;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hackerprank.generator")
class ProblemGenerationProperties {
    private String provider = "deterministic";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    boolean useOpenAi() {
        return "openai".equals(normalizedProvider());
    }

    private String normalizedProvider() {
        if (provider == null || provider.isBlank()) {
            return "deterministic";
        }

        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
