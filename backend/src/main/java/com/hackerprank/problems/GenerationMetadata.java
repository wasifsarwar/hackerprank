package com.hackerprank.problems;

public record GenerationMetadata(
    String provider,
    String modelId,
    String promptVersion,
    String promptText,
    String parametersJson,
    String validationStatus,
    String validationErrors,
    String validationSummary,
    String intendedTechnique
) {
    public static GenerationMetadata deterministic(String topic, String difficulty, String intendedTechnique) {
        String normalizedTopic = topic == null || topic.isBlank() ? "arrays" : topic;
        String normalizedDifficulty = difficulty == null || difficulty.isBlank() ? "Easy" : difficulty;
        return new GenerationMetadata(
            "deterministic",
            "template-v1",
            "deterministic-v1",
            "Select a built-in HackerPrank template for the requested topic and difficulty.",
            "{\"topic\":\"" + escapeJson(normalizedTopic) + "\",\"difficulty\":\"" + escapeJson(normalizedDifficulty) + "\"}",
            "PENDING",
            "",
            "",
            intendedTechnique == null ? "" : intendedTechnique
        );
    }

    public GenerationMetadata withValidation(GeneratedProblemValidationReport report) {
        return new GenerationMetadata(
            provider,
            modelId,
            promptVersion,
            promptText,
            parametersJson,
            report.status(),
            report.errors(),
            report.summary(),
            intendedTechnique
        );
    }

    static GenerationMetadata empty() {
        return new GenerationMetadata("", "", "", "", "", "", "", "", "");
    }

    private static String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
