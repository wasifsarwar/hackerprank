package com.hackerprank.editor;

import java.util.List;

public record JavaCompletionResponse(
    boolean enabled,
    String source,
    String message,
    List<JavaCompletionItem> items
) {
    static JavaCompletionResponse disabled(String message) {
        return new JavaCompletionResponse(false, "jdtls", message, List.of());
    }

    static JavaCompletionResponse enabled(List<JavaCompletionItem> items) {
        return new JavaCompletionResponse(true, "jdtls", "", items);
    }
}
