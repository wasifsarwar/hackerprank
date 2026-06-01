package com.hackerprank.problems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GenerationUsageEstimatorTests {
    @Test
    void estimatesTokensAndHashesPromptAndResponseWithoutStoringRawText() {
        GenerationUsageMetrics metrics = GenerationUsageEstimator.from("abcd efgh", "generated payload");

        assertEquals(9, metrics.promptCharCount());
        assertEquals(17, metrics.responseCharCount());
        assertEquals(3, metrics.estimatedPromptTokens());
        assertEquals(5, metrics.estimatedResponseTokens());
        assertEquals(64, metrics.promptHash().length());
        assertEquals(64, metrics.responseHash().length());
        assertFalse(metrics.promptHash().contains("abcd"));
    }

    @Test
    void keepsEmptyInputsAsZeroUsage() {
        GenerationUsageMetrics metrics = GenerationUsageEstimator.from(null, "");

        assertEquals("", metrics.promptHash());
        assertEquals("", metrics.responseHash());
        assertEquals(0, metrics.promptCharCount());
        assertEquals(0, metrics.responseCharCount());
        assertEquals(0, metrics.estimatedPromptTokens());
        assertEquals(0, metrics.estimatedResponseTokens());
        assertTrue(metrics.equals(GenerationUsageMetrics.empty()));
    }
}
