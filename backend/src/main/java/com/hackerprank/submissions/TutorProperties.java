package com.hackerprank.submissions;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "hackerprank.tutor")
class TutorProperties {
    private String provider = "deterministic";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    boolean prefersOpenAi() {
        return "openai".equals(provider == null ? "" : provider.toLowerCase(Locale.ROOT));
    }
}
