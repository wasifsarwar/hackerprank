package com.hackerprank.problems;

import java.util.List;

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
        return deterministic(topic, difficulty, List.of(), "", "Classic", intendedTechnique);
    }

    public static GenerationMetadata deterministic(
        String topic,
        String difficulty,
        List<String> targetConcepts,
        String constraintsNotes,
        String interviewStyle,
        String intendedTechnique
    ) {
        String normalizedTopic = topic == null || topic.isBlank() ? "arrays" : topic;
        String normalizedDifficulty = difficulty == null || difficulty.isBlank() ? "Easy" : difficulty;
        String normalizedConstraintsNotes = constraintsNotes == null ? "" : constraintsNotes;
        String normalizedInterviewStyle = interviewStyle == null || interviewStyle.isBlank() ? "Classic" : interviewStyle;
        return new GenerationMetadata(
            "deterministic",
            "template-v1",
            "deterministic-v1",
            "Select a built-in HackerPrank template for the requested topic and difficulty.",
            parametersJson(
                normalizedTopic,
                normalizedDifficulty,
                targetConcepts == null ? List.of() : targetConcepts,
                normalizedConstraintsNotes,
                normalizedInterviewStyle
            ),
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

    public boolean repairUsed() {
        String privateParameters = parametersJson == null ? "" : parametersJson;
        String privatePrompt = promptText == null ? "" : promptText;
        return privateParameters.contains("\"repairUsed\":true") || privatePrompt.contains("Repair context:");
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

    private static String parametersJson(
        String topic,
        String difficulty,
        List<String> targetConcepts,
        String constraintsNotes,
        String interviewStyle
    ) {
        return "{"
            + "\"topic\":\"" + escapeJson(topic) + "\","
            + "\"difficulty\":\"" + escapeJson(difficulty) + "\","
            + "\"targetConcepts\":" + jsonStringArray(targetConcepts) + ","
            + "\"constraintsNotes\":\"" + escapeJson(constraintsNotes) + "\","
            + "\"interviewStyle\":\"" + escapeJson(interviewStyle) + "\""
            + "}";
    }

    private static String jsonStringArray(List<String> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(",");
            }
            String value = values.get(index) == null ? "" : values.get(index);
            builder.append("\"").append(escapeJson(value)).append("\"");
        }
        return builder.append("]").toString();
    }
}
