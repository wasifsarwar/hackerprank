package com.hackerprank.problems;

public record PublicGenerationMetadata(
    String provider,
    String modelId,
    String promptVersion,
    String validationStatus,
    String validationSummary,
    String intendedTechnique
) {
    public static PublicGenerationMetadata from(GenerationMetadata metadata) {
        if (metadata == null) {
            return new PublicGenerationMetadata("", "", "", "", "", "");
        }

        return new PublicGenerationMetadata(
            metadata.provider(),
            metadata.modelId(),
            metadata.promptVersion(),
            metadata.validationStatus(),
            metadata.validationSummary(),
            metadata.intendedTechnique()
        );
    }
}
