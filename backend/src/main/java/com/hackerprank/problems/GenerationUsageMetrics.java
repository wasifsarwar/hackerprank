package com.hackerprank.problems;

public record GenerationUsageMetrics(
    String promptHash,
    String responseHash,
    int promptCharCount,
    int responseCharCount,
    int estimatedPromptTokens,
    int estimatedResponseTokens
) {
    static GenerationUsageMetrics empty() {
        return new GenerationUsageMetrics("", "", 0, 0, 0, 0);
    }
}
