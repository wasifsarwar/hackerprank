package com.hackerprank.problems;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class GenerationUsageEstimator {
    private GenerationUsageEstimator() {
    }

    static GenerationUsageMetrics from(String promptText, String responseText) {
        String prompt = promptText == null ? "" : promptText;
        String response = responseText == null ? "" : responseText;
        return new GenerationUsageMetrics(
            sha256(prompt),
            sha256(response),
            prompt.length(),
            response.length(),
            estimateTokens(prompt),
            estimateTokens(response)
        );
    }

    private static int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        return Math.max(1, (text.length() + 3) / 4);
    }

    private static String sha256(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is unavailable", exception);
        }
    }
}
