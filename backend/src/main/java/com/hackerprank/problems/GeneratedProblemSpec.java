package com.hackerprank.problems;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record GeneratedProblemSpec(
    String topic,
    String difficulty,
    Problem problem,
    Map<String, String> referenceSolutions,
    GenerationMetadata generationMetadata
) {
    public GeneratedProblemSpec {
        referenceSolutions = referenceSolutions == null
            ? Map.of()
            : Collections.unmodifiableMap(new LinkedHashMap<>(referenceSolutions));
        generationMetadata = generationMetadata == null ? GenerationMetadata.empty() : generationMetadata;
    }
}
